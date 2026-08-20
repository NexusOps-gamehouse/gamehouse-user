package gg.duo.user.client;

import gg.duo.common.exception.BusinessException;
import gg.duo.common.exception.ErrorCode;
import gg.duo.user.dto.RiotProfileView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.NoSuchElementException;

/**
 * riot 서비스 호출 — 클러스터 내부 전용.
 *
 * 예전에는 RiotService 를 그냥 주입받아 함수로 불렀다. 서비스가 나뉘면 그 호출이
 * 네트워크를 건너간다. 바뀌는 것은 세 가지다.
 *   1) 상대가 죽어 있을 수 있다 → ResourceAccessException
 *   2) 상대가 느릴 수 있다      → 타임아웃
 *   3) 상대의 4xx/5xx 를 우리 응답으로 옮겨 담아야 한다
 * 함수 호출에는 없던 실패 모드이므로 여기서 전부 처리해 위로 올린다.
 *
 * base-url 은 k8s 서비스 이름(http://riot:8080)이다. riot 은 Ingress 에
 * 노출하지 않으므로 클러스터 밖에서는 이 경로에 닿을 수 없다.
 */
@Component
public class RiotClient {

    private final RestClient restClient;

    public RiotClient(RestClient.Builder builder,
                      @Value("${services.riot.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /** Riot ID 로 프로필 조회 (riot 이 Account API 부터 탄다) */
    public RiotProfileView fetchProfile(String gameName, String tagLine) {
        return call(uri -> uri.path("/internal/riot/profile")
                .queryParam("gameName", gameName)
                .queryParam("tagLine", tagLine)
                .build());
    }

    /** puuid 를 이미 알 때 — riot 이 Account API 호출을 건너뛴다 */
    public RiotProfileView fetchProfileByPuuid(String puuid, String gameName, String tagLine) {
        return call(uri -> uri.path("/internal/riot/profile/by-puuid")
                .queryParam("puuid", puuid)
                .queryParam("gameName", gameName)
                .queryParam("tagLine", tagLine)
                .build());
    }

    private RiotProfileView call(java.util.function.Function<
            org.springframework.web.util.UriBuilder, java.net.URI> uriSpec) {
        try {
            return restClient.get()
                    .uri(uriSpec)
                    .retrieve()
                    // 없는 계정 — riot 의 404 를 그대로 우리 404 로 옮긴다.
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        if (res.getStatusCode().value() == 429) {
                            throw new BusinessException(ErrorCode.RATE_LIMITED);
                        }
                        throw new NoSuchElementException(
                                "소환사를 찾을 수 없습니다. 게임명과 태그를 확인해 주세요.");
                    })
                    .body(RiotProfileView.class);
        } catch (ResourceAccessException e) {
            // riot 파드가 내려갔거나 응답이 없다. 사용자에게는 5xx 가 아니라
            // "지금은 연동을 못 한다"로 알려야 로그인·글쓰기까지 막힌 것처럼 보이지 않는다.
            throw new BusinessException(ErrorCode.UPSTREAM_UNAVAILABLE,
                    "라이엇 연동 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.");
        }
    }
}
