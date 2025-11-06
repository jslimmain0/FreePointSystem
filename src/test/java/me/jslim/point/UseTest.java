package me.jslim.point;

import me.jslim.point.application.dto.EarnCmd;
import me.jslim.point.application.dto.EarnResult;
import me.jslim.point.application.dto.UseCmd;
import me.jslim.point.application.dto.UseResult;
import me.jslim.point.application.service.PointCommandService;
import me.jslim.point.domain.entity.PointUse;
import me.jslim.point.domain.repository.PointUseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("사용")
public class UseTest {
    @Autowired
    PointCommandService pointCommandService;

    @Autowired
    PointUseRepository useRepo;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void resetDb() {
        jdbc.batchUpdate("""
            SET REFERENTIAL_INTEGRITY FALSE;
            TRUNCATE TABLE POINT;
            TRUNCATE TABLE POINT_EARN;
            TRUNCATE TABLE POINT_EARN_CANCEL;
            TRUNCATE TABLE POINT_USE;
            TRUNCATE TABLE POINT_USE_CANCEL;
            TRUNCATE TABLE POINT_WALLET;
            SET REFERENTIAL_INTEGRITY TRUE;
        """.split(";"));
    }


    @Test
    @DisplayName("포인트 사용시에는 주문번호를 함께 기록하여 어떤 주문에서 얼마의 포인트를 사용했는지 식별할 수 있어야 한다.")
    void use1() {
        // given
        String userId = "jslim";
        String orderNo = "ABC123";
        LocalDate now = LocalDate.now();
        EarnCmd earnCmd = new EarnCmd(userId, 1000L, null, null);
        UseCmd useCmd1 = new UseCmd(userId, 111L, orderNo);

        // when
        EarnResult earnResult = pointCommandService.earn(earnCmd, now);
        UseResult useResult1 = pointCommandService.use(useCmd1, now);

        // then
        assertThat(earnResult.balanceAmount()).isEqualTo(1000L);
        assertThat(useResult1.balanceAmount()).isEqualTo(889L);

        // 주문번호로 추적 + 포인트 사용확인
        Long amount = jdbc.queryForObject("""
            SELECT AMOUNT
              FROM POINT_USE
             WHERE ORDER_NUMBER = ?
        """, Long.class, orderNo);
        assertThat(amount).isEqualTo(111L);
    }

    @Test
    @DisplayName("포인트 사용시에는 관리자가 수기 지급한 포인트가 우선 사용되어야 하며, 만료일이 짧게 남은 순서로 사용해야 한다.")
    void use2() {
        // given
        // 기본 값 최대 적립 가능 포인트 1,000,000
        String userId = "jslim";
        LocalDate now = LocalDate.now();
        EarnCmd earnCmd1 = new EarnCmd(userId, 10_000L, null, "20260101");
        EarnCmd earnCmd2 = new EarnCmd(userId, 20_000L, null, "20260102");
        EarnCmd earnCmd3 = new EarnCmd(userId, 20_000L, "EARN_MANUAL", "20260103");
        UseCmd useCmd1 = new UseCmd(userId, 21_000L, "ABC111");
        UseCmd useCmd2 = new UseCmd(userId, 20_000L, "ABC222");

        // when
        EarnResult earnResult1 = pointCommandService.earn(earnCmd1, now);
        EarnResult earnResult2 = pointCommandService.earn(earnCmd2, now);
        EarnResult earnResult3 = pointCommandService.earn(earnCmd3, now);
        UseResult useResult1 = pointCommandService.use(useCmd1, now);
        UseResult useResult2 = pointCommandService.use(useCmd2, now);

        // then
        assertThat(earnResult1.balanceAmount()).isEqualTo(10_000L);
        assertThat(earnResult2.balanceAmount()).isEqualTo(30_000L);
        assertThat(earnResult3.balanceAmount()).isEqualTo(50_000L);
        assertThat(useResult1.balanceAmount()).isEqualTo(29_000L);
        assertThat(useResult2.balanceAmount()).isEqualTo(9_000L);

        // 1번 사용에 대한 사용 상세
        List<PointUse> pointUseList = useRepo.findAllByPointKey(useResult1.pointKey());
        assertThat(pointUseList).isNotEmpty();
        assertThat(pointUseList).hasSize(2); // 관리자적립에서 2만원, 마감기한이 먼저인 1번적립건 1000원 사용 2건
        assertThat(pointUseList.getFirst().getAmount()).isEqualTo(20_000L);
        assertThat(pointUseList.get(1).getAmount()).isEqualTo(1_000L);

        // 2번 사용에 대한 사용 상세
        List<PointUse> pointUseList2 = useRepo.findAllByPointKey(useResult2.pointKey());
        assertThat(pointUseList2).isNotEmpty();
        assertThat(pointUseList2).hasSize(2); // 1번적립건 9000원 마저 사용 나머지 11000원 2번적립에서 사용
        assertThat(pointUseList2.getFirst().getAmount()).isEqualTo(9_000);
        assertThat(pointUseList2.get(1).getAmount()).isEqualTo(11_000L);
    }
}
