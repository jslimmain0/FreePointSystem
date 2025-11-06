package me.jslim.point;

import me.jslim.point.application.dto.*;
import me.jslim.point.application.service.PointCommandService;
import me.jslim.point.application.service.PointExpireService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("사용취소")
public class CancelUseTest {
    @Autowired
    PointCommandService pointCommandService;

    @Autowired
    PointExpireService pointExpireService;

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
    @DisplayName("사용한 금액중 전체 사용취소")
    void useCancel1() {
        // given
        String userId = "jslim";
        LocalDate now = LocalDate.now();
        EarnCmd earnCmd = new EarnCmd(userId, 10_000L, null, null);
        UseCmd useCmd1 = new UseCmd(userId, 1_000L, "ABC123");

        // when
        EarnResult earnResult = pointCommandService.earn(earnCmd, now);
        UseResult useResult1 = pointCommandService.use(useCmd1, now);
        UseCancelResult useCancelResult = pointCommandService.cancelUse(
                new UseCancelCmd(userId, useResult1.pointKey(), useCmd1.useAmount()
        ));

        // then
        assertThat(earnResult.balanceAmount()).isEqualTo(10_000L);
        assertThat(useResult1.balanceAmount()).isEqualTo(9_000L);
        assertThat(useCancelResult.balanceAmount()).isEqualTo(10_000L);
    }

    @Test
    @DisplayName("사용한 금액중 일부를 사용취소 할수 있다.")
    void useCancel2() {
        // given
        String userId = "jslim";
        LocalDate now = LocalDate.now();
        EarnCmd earnCmd = new EarnCmd(userId, 10_000L, null, null);
        UseCmd useCmd1 = new UseCmd(userId, 1_000L, "ABC123");

        // when
        EarnResult earnResult = pointCommandService.earn(earnCmd, now);
        UseResult useResult1 = pointCommandService.use(useCmd1, now);
        UseCancelResult useCancelResult = pointCommandService.cancelUse(
                new UseCancelCmd(userId, useResult1.pointKey(), 500L
        ));

        // then
        assertThat(earnResult.balanceAmount()).isEqualTo(10_000L);
        assertThat(useResult1.balanceAmount()).isEqualTo(9_000L);
        assertThat(useCancelResult.balanceAmount()).isEqualTo(9_500L);
    }


    @Test
    @DisplayName("사용취소 시점에 이미 만료된 포인를 사용취소 해야 한다면 그 금액만큼 신규적립 처리 한다.")
    void useCancel3() {
        // given
        String userId = "jslim";
        LocalDate now = LocalDate.now();
        // 5일뒤
        String expireDate1 = now.plusDays(5)
                .format(DateTimeFormatter.BASIC_ISO_DATE);
        EarnCmd earnCmd = new EarnCmd(userId, 10_000L, null, expireDate1);
        UseCmd useCmd1 = new UseCmd(userId, 1_000L, "ABC123");

        // when
        EarnResult earnResult = pointCommandService.earn(earnCmd, now);
        UseResult useResult1 = pointCommandService.use(useCmd1, now);

        pointExpireService.findActiveExpiring(now.plusDays(6)); // 적립을 만료시키기 위해 6일 이 지난 걸로 설정
        UseCancelResult useCancelResult = pointCommandService.cancelUse(
                new UseCancelCmd(userId, useResult1.pointKey(), 500L
        ));

        // then
        assertThat(earnResult.balanceAmount()).isEqualTo(10_000L);
        assertThat(useResult1.balanceAmount()).isEqualTo(9_000L);
        assertThat(useCancelResult.balanceAmount()).isEqualTo(9_500L);

        // 사용 취소로 인한 적립건 확인
        String newEarnPointKey = jdbc.queryForObject("""
            SELECT POINT_KEY
              FROM POINT_EARN
            WHERE REF_USE_CANCEL_KEY = ( SELECT USE_CANCEL_KEY FROM POINT_USE_CANCEL WHERE POINT_KEY = ?)
        """, String.class, useCancelResult.pointKey());
        
        // 만료된 적립과는 다른건
        assertThat(newEarnPointKey).isNotEqualTo(earnResult.pointKey());
    }
}
