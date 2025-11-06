package me.jslim.point;

import me.jslim.point.application.dto.EarnCmd;
import me.jslim.point.application.dto.EarnResult;
import me.jslim.point.application.dto.UseCmd;
import me.jslim.point.application.dto.UseResult;
import me.jslim.point.application.service.PointCommandService;
import me.jslim.point.application.support.PointPolicy;
import me.jslim.point.domain.entity.PointEarn;
import me.jslim.point.domain.entity.PointUse;
import me.jslim.point.domain.repository.PointEarnRepository;
import me.jslim.point.domain.repository.PointUseRepository;
import me.jslim.point.global.exception.BusinessException;
import me.jslim.point.global.vo.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@DisplayName("적립")
public class EarnTest {
    @Autowired
    PointCommandService pointCommandService;

    @Autowired
    PointUseRepository useRepo;

    @Autowired
    PointEarnRepository earnRepo;

    @Autowired
    PointPolicy pointPolicy;

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
    @DisplayName("1회 적립가능 포인트는 1포인트 이상")
    void earn1() {
        // given
        String userId = "jslim";
        LocalDate earnDate = LocalDate.now();
        EarnCmd earnCmd = new EarnCmd(userId, 0L, null, null);

        // when
        BusinessException exception = assertThrows(BusinessException.class, () -> pointCommandService.earn(earnCmd, earnDate));

        // then
        assertThat(exception.getResultCode()).isEqualTo(ResultCode.WALLET_AMOUNT_ERR);
    }

    @Test
    @DisplayName("1회 적립가능 포인트는 10만포인트 이하")
    void earn2() {
        // given
        String userId = "jslim";
        LocalDate earnDate = LocalDate.now();
        EarnCmd earnCmd = new EarnCmd(userId, 100_001L, null, null);

        // when
        BusinessException exception = assertThrows(BusinessException.class, () -> pointCommandService.earn(earnCmd, earnDate));

        // then
        assertThat(exception.getResultCode()).isEqualTo(ResultCode.POINT_AMOUNT_ERR);
    }

    @Test
    @DisplayName("적립가능 포인트는 하드코딩이 아닌 방법으로 제어할수 있어야 한다")
    void earn3() {
        // given
        String userId = "jslim";
        LocalDate earnDate = LocalDate.now();
        EarnCmd earnCmd = new EarnCmd(userId, 100_001L, null, null);

        // when
        pointPolicy.update("MAXIMUM_POINT", "110000"); // 적립 가능 포인트 변경
        EarnResult earnResult = pointCommandService.earn(earnCmd, earnDate);

        // then
        assertThat(earnResult.balanceAmount()).isEqualTo(100_001L);


    }


    @Test
    @DisplayName("개인별로 보유 가능한 무료포인트의 최대금액 제한이 존재한다")
    void earn4() {
        // given
        // 기본 값 최대 적립 가능 포인트 1,000,000
        String userId = "jslim";
        LocalDate earnDate = LocalDate.now();
        EarnCmd earnCmd1 = new EarnCmd(userId, 900_000L, null, null);
        EarnCmd earnCmd2 = new EarnCmd(userId, 100_001L, null, null);

        // when
        pointPolicy.update("MAXIMUM_POINT", "1000000"); // 1회 적립 가능 포인트 변경 (최대 적립가능포인트를 빨리 넘기기 위해)
        EarnResult earnResult = pointCommandService.earn(earnCmd1, earnDate);
        BusinessException exception = assertThrows(BusinessException.class, () -> pointCommandService.earn(earnCmd2, earnDate));

        // then
        assertThat(earnResult.balanceAmount()).isEqualTo(900_000L);
        assertThat(exception.getResultCode()).isEqualTo(ResultCode.WALLET_AMOUNT_ERR);
    }


    @Test
    @DisplayName("개인별로 보유 가능한 무료포인트의 최대금액 제한을 하드코딩이 아닌 별도의 방법으로 변경할 수 있어야 한다.")
    void earn5() {
        // given
        // 기본 값 최대 적립 가능 포인트 1,000,000
        String userId = "jslim";
        LocalDate earnDate = LocalDate.now();
        EarnCmd earnCmd1 = new EarnCmd(userId, 900_000L, null, null);
        EarnCmd earnCmd2 = new EarnCmd(userId, 100_001L, null, null);

        // when
        pointPolicy.update("MAXIMUM_POINT", "1000000"); // 1회 적립 가능 포인트 변경 (최대 적립가능포인트를 빨리 넘기기 위해)
        EarnResult earnResult = pointCommandService.earn(earnCmd1, earnDate);
        BusinessException exception = assertThrows(BusinessException.class, () -> pointCommandService.earn(earnCmd2, earnDate));

        // 최대 적립가능 포인트 변경 (실제 서비스에서는 관리자 변경이나, userGrade에 따라서 번경하면 됨)
        jdbc.update("""
            UPDATE POINT_WALLET
               SET MAXIMUM_AMOUNT = 9000000
             WHERE USER_ID = ?
        """, userId);
        // 재시도
        EarnResult earnResult2 = pointCommandService.earn(earnCmd2, earnDate);


        // then
        assertThat(earnResult.balanceAmount()).isEqualTo(900_000L);
        assertThat(exception.getResultCode()).isEqualTo(ResultCode.WALLET_AMOUNT_ERR);
        assertThat(earnResult2.balanceAmount()).isEqualTo(1_000_001L);
    }

    @Test
    @DisplayName("특정 시점에 적립된 포인트는 1원단위까지 어떤 주문에서 사용되었는지 추적할수 있어야 한다.")
    void earn6() {
        // given
        // 기본 값 최대 적립 가능 포인트 1,000,000
        String userId = "jslim";
        LocalDate now = LocalDate.now();
        EarnCmd earnCmd1 = new EarnCmd(userId, 50_000L, null, "20260101");
        EarnCmd earnCmd2 = new EarnCmd(userId, 60_000L, null, "20260102");
        UseCmd useCmd1 = new UseCmd(userId, 60_111L, "ABC123");

        // when
        EarnResult earnResult1 = pointCommandService.earn(earnCmd1, now);
        EarnResult earnResult2 = pointCommandService.earn(earnCmd2, now);
        UseResult useResult1 = pointCommandService.use(useCmd1, now);

        // then
        // 1번 사용에 대한 사용 상세
        List<PointUse> pointUseList = useRepo.findAllByPointKey(useResult1.pointKey());
        assertThat(pointUseList).isNotEmpty();
        assertThat(pointUseList).hasSize(2); // 5만원 적립건에서 5만원, 6만원 적립건에서 10,111원 사용으로 2건
        assertThat(pointUseList.getFirst().getAmount()).isEqualTo(50_000L);
        assertThat(pointUseList.get(1).getAmount()).isEqualTo(10_111L);

        // 해당 적립건 추적
        Optional<PointEarn> pointEarn1 = earnRepo.findById(pointUseList.getFirst().getEarnKey());
        Optional<PointEarn> pointEarn2 = earnRepo.findById(pointUseList.get(1).getEarnKey());
        assertThat(pointEarn1).isPresent();
        assertThat(pointEarn2).isPresent();

        // 적립건의 PointKey가 응답받은 내용과 동일해야함
        PointEarn earn1 = pointEarn1.get();
        PointEarn earn2 = pointEarn2.get();
        assertThat(earn1.getPointKey()).isEqualTo(earnResult1.pointKey());
        assertThat(earn2.getPointKey()).isEqualTo(earnResult2.pointKey());
    }

    @Test
    @DisplayName("포인트 적립은 관리자가 수기로 지급할 수 있으며, 수기지급한 포인트는 다른 적립과 구분되어 식별할 수 있어야 한다.")
    void earn7() {
        // given
        String userId = "jslim";
        LocalDate now = LocalDate.now();
        EarnCmd earnCmd1 = new EarnCmd(userId, 10_001L, "EARN_MANUAL", null);
        EarnCmd earnCmd2 = new EarnCmd(userId, 10_000L, null, null);

        // when
        EarnResult earnResult1 = pointCommandService.earn(earnCmd1, now);
        EarnResult earnResult2 = pointCommandService.earn(earnCmd2, now);

        // then
        Optional<PointEarn> pointEarn1 = earnRepo.findByPointKey(earnResult1.pointKey());
        assertThat(pointEarn1).isPresent();
        assertThat(pointEarn1.get().isManual()).isTrue();

        Optional<PointEarn> pointEarn2 = earnRepo.findByPointKey(earnResult2.pointKey());
        assertThat(pointEarn2).isPresent();
        assertThat(pointEarn2.get().isManual()).isFalse();
    }


    @Test
    @DisplayName("모든 포인트는 만료일이 존재하며, 최소 1일이상 최대 5년 미만의 만료일을 부여할 수 있다. (기본 365일)")
    void earn8() {
        // given
        String userId = "jslim";
        LocalDate now = LocalDate.now();

        // 5년뒤
        String expireDate1 = now.plusDays(365*5)
                .format(DateTimeFormatter.BASIC_ISO_DATE);

        // 5년하고 하루 뒤
        String expireDate2 = now.plusDays(365*5)
                .plusDays(1)
                .format(DateTimeFormatter.BASIC_ISO_DATE);
        
        EarnCmd earnCmd1 = new EarnCmd(userId, 10_001L, null, null);
        EarnCmd earnCmd2 = new EarnCmd(userId, 10_000L, null, expireDate1);
        EarnCmd earnCmd3 = new EarnCmd(userId, 10_000L, null, expireDate2);

        // when
        EarnResult earnResult1 = pointCommandService.earn(earnCmd1, now);
        EarnResult earnResult2 = pointCommandService.earn(earnCmd2, now);
        BusinessException exception = assertThrows(BusinessException.class, () -> pointCommandService.earn(earnCmd3, now));


        // then
        Optional<PointEarn> pointEarn1 = earnRepo.findByPointKey(earnResult1.pointKey());
        assertThat(pointEarn1).isPresent();
        assertThat(pointEarn1.get().getExpireDate()).isEqualTo(now.plusDays(365)); // 미설정 시 365

        Optional<PointEarn> pointEarn2 = earnRepo.findByPointKey(earnResult2.pointKey());
        assertThat(pointEarn2).isPresent();
        assertThat(pointEarn2.get().getExpireDate()).isEqualTo(now.plusDays(365*5)); // 5년뒤

        assertThat(exception.getResultCode()).isEqualTo(ResultCode.EARN_MAX_EXPIRE_DATE_ERROR); // 최대 만료일 에러

    }
}
