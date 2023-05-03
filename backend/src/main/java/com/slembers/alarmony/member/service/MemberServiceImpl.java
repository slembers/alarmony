package com.slembers.alarmony.member.service;

import com.slembers.alarmony.global.execption.CustomException;
import com.slembers.alarmony.global.jwt.JwtTokenProvider;
import com.slembers.alarmony.global.redis.service.RedisUtil;
import com.slembers.alarmony.member.dto.request.FindMemberIdDto;
import com.slembers.alarmony.member.dto.request.ReissueTokenDto;
import com.slembers.alarmony.member.dto.request.SignUpDto;
import com.slembers.alarmony.member.dto.response.CheckDuplicateDto;
import com.slembers.alarmony.member.dto.response.TokenResponseDto;
import com.slembers.alarmony.member.entity.Member;
import com.slembers.alarmony.member.exception.MemberErrorCode;
import com.slembers.alarmony.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements MemberService {

    private final ModelMapper modelMapper;

    private final MemberRepository memberRepository;

    private final EmailVerifyService emailVerifyService;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider jwtTokenProvider;

    private final RedisUtil redisUtil;

    /**
     * 회원가입
     *
     * @param signUpDto 회원가입 정보
     */
    @Transactional
    @Override
    public void signUp(SignUpDto signUpDto) {

        checkDuplicatedField(signUpDto);

        Member member = modelMapper.map(signUpDto, Member.class);

        member.encodePassword(passwordEncoder);

        emailVerifyService.sendVerificationMail(member.getUsername(), member.getEmail());

        memberRepository.save(member);

    }

    /**
     * 아이디 중복체크
     *
     * @param username 유저 아이디
     * @return 존재여부
     **/


    @Override
    public CheckDuplicateDto checkForDuplicateId(String username) {

        return CheckDuplicateDto.builder().isDuplicated(memberRepository.existsByUsername(username)).build();

    }

    /**
     * 이메일 중복 체크
     *
     * @param email :이메일주소
     * @return 존재여부
     */
    @Override
    public CheckDuplicateDto checkForDuplicateEmail(String email) {

        //TODO: 얘 리펙토링 필요한거 같음 리턴을 dto로 하면 안될듯
        return CheckDuplicateDto.builder().isDuplicated(memberRepository.existsByEmail(email)).build();
    }


    /**
     * 닉네임 중복 체크
     *
     * @param nickname : 닉네임
     * @return 존재여부
     */

    @Override
    public CheckDuplicateDto checkForDuplicateNickname(String nickname) {
        return CheckDuplicateDto.builder().isDuplicated(memberRepository.existsByNickname(nickname)).build();
    }

    /**
     * 회원 가입시 발생하는 중복 체크
     *
     * @param signUpDto: 회원 가입 정보
     */

    private void checkDuplicatedField(SignUpDto signUpDto) {

        //아이디 중복 체크
        if (checkForDuplicateId(signUpDto.getUsername()).isDuplicated())
            throw new CustomException(MemberErrorCode.ID_DUPLICATED);
        //닉네임 중복 체크
        if (checkForDuplicateNickname(signUpDto.getNickname()).isDuplicated())
            throw new CustomException(MemberErrorCode.NICKNAME_DUPLICATED);
        //이메일 중복 체크
        if (checkForDuplicateEmail(signUpDto.getEmail()).isDuplicated())
            throw new CustomException(MemberErrorCode.EMAIL_DUPLICATED);

    }

    @Override
    public TokenResponseDto reissueToken(ReissueTokenDto reissueTokenDto) {

        //예외처리
        jwtTokenProvider.validRefreshToken(reissueTokenDto.getRefreshToken());

        String redisRefreshToken = redisUtil.getData("Refresh:" + reissueTokenDto.getUsername());

        Member member = memberRepository.findByUsername(reissueTokenDto.getUsername()).orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (redisRefreshToken.equals(redisRefreshToken)) { //일치할때만 재발급

            String accessToken = jwtTokenProvider.generateAccessToken(member);
            String refreshToken = jwtTokenProvider.generateRefreshToken(member);

            return new TokenResponseDto("bearer", accessToken, refreshToken);

        } else {  //일치하지 않으면
            log.error("[Refresh Controller] Refresh Token값이 일치하지 않습니다.");
        }

        return null;
    }

    @Override
    public void putRegistrationToken(String username, String registrationToken) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));
        if (registrationToken == null || registrationToken.length() == 0)
            throw new CustomException(MemberErrorCode.MEMBER_REGISTRATION_TOKEN_WRONG);

        try {
            member.modifyToken(registrationToken);
            memberRepository.save(member);
        } catch (Exception e) {
            throw new CustomException(MemberErrorCode.MEMBER_REGISTRATION_TOKEN_WRONG);
        }
    }

    /**
     * 아이디 찾기
     *
     * @param findMemberIdDto 회원 이메일
     */

    @Override
    public void findMemberId(FindMemberIdDto findMemberIdDto) throws MessagingException {

        Member member = memberRepository.findMemberByEmail(findMemberIdDto.getEmail())
                .orElseThrow(() -> new CustomException(MemberErrorCode.EMAIL_NOT_FOUND));

        Map<String, String> values = new HashMap<>();
        values.put("username", member.getUsername());

        emailVerifyService.sendTemplateEmail("알라모니 아이디 찾기", findMemberIdDto.getEmail(), "findId",values);

    }
}
