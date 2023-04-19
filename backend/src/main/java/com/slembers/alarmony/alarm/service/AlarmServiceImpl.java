package com.slembers.alarmony.alarm.service;

import com.slembers.alarmony.alarm.dto.AlarmDto;
import com.slembers.alarmony.alarm.dto.response.AlarmListResponseDto;
import com.slembers.alarmony.alarm.entity.Alarm;
import com.slembers.alarmony.alarm.entity.MemberAlarm;
import com.slembers.alarmony.alarm.repository.AlarmRepository;
import com.slembers.alarmony.alarm.repository.MemberAlarmRepository;
import com.slembers.alarmony.global.util.CommonMethods;
import com.slembers.alarmony.member.entity.Member;
import com.slembers.alarmony.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmServiceImpl implements AlarmService {

    private AlarmRepository alarmRepository;

    private MemberAlarmRepository memberAlarmRepository;

    private MemberRepository memberRepository;

    /**
     * 유저네임을 기준으로 멤버알람 리스트를 가져오고, 이를 responseDTO에 담는다.
     *
     * @param username
     * @return
     */
    @Override
    public AlarmListResponseDto getAlarmList(String username) {
        Member member = memberRepository.findByUsername(username)
                // 없으면 멤버 없음 예외 던지는 코드로 바꿀 것
                .orElseThrow();

        try {
            List<MemberAlarm> memberAlarmList = memberAlarmRepository.findAllByMember(member);
            List<AlarmDto> alarms = new ArrayList<>();

            memberAlarmList.forEach((memberAlarm) -> {
                Alarm alarm = memberAlarm.getAlarm();
                LocalTime localTime = alarm.getTime();

                AlarmDto alarmDto = AlarmDto.builder()
                        .alarmId(memberAlarm.getId())
                        .title(alarm.getTitle())
                        .time(localTime.getHour() % 12 == 0 ? localTime : localTime.minusHours(12))
                        .ampm(alarm.getTime().getHour() % 12 == 0 ? "오전" : "오후")
                        .alarmDate(CommonMethods.changeByteListToStringList(alarm.getAlarmDate()))
                        .build();

                alarms.add(alarmDto);
            });

            return AlarmListResponseDto.builder().alarms(alarms).build();
        } catch (Exception e) {
            // 에러 만들어지면 바꿀 것
            throw new RuntimeException();
        }
    }

    /**
     * 알람 id를 사용하여 데이터베이스에서 알람 객체를 얻어온다.
     * 알람 정보가 존재하지 않을 경우 예외를 던진다.
     *
     * @param id
     * @return 알람 객체
     */
    @Override
    public Alarm getAlarmByAlarmId(Long id) {
        return alarmRepository.findById(id).orElseThrow(() -> new RuntimeException());
    }
}
