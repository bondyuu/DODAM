package com.team1.dodam.service;

import com.team1.dodam.domain.CertificationNumber;
import com.team1.dodam.domain.RefreshToken;
import com.team1.dodam.dto.TokenDto;
import com.team1.dodam.dto.response.EditProfileResponseDto;
import com.team1.dodam.dto.response.LoginResponseDto;
import com.team1.dodam.dto.response.MessageResponseDto;
import com.team1.dodam.dto.response.ResponseDto;
import com.team1.dodam.domain.User;
import com.team1.dodam.domain.UserDetailsImpl;
import com.team1.dodam.dto.request.*;
import com.team1.dodam.global.error.ErrorCode;
import com.team1.dodam.jwt.TokenProvider;
import com.team1.dodam.repository.CertificationNumberRepository;
import com.team1.dodam.repository.RefreshTokenRepository;
import com.team1.dodam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final CertificationNumberRepository certificationNumberRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final S3UploadService s3UploadService;

    @Transactional
    public ResponseDto<?> signup(SignupRequestDto requestDto) {


        if (isPresentEmail(requestDto.getEmail()) != null) {
            return ResponseDto.fail(ErrorCode.DUPLICATED_EMAIL);
        }

        if (isPresentNickname(requestDto.getNickname()) != null) {
            return ResponseDto.fail(ErrorCode.DUPLICATED_NICKNAME);
        }

        if (!requestDto.getPassword().equals(requestDto.getPasswordConfirm())) {
            return ResponseDto.fail(ErrorCode.PASSWORDS_NOT_MATCHED);
        }


        User user = User.builder()
                        .email(requestDto.getEmail())
                        .nickname(requestDto.getNickname())
                        .password(passwordEncoder.encode(requestDto.getPassword()))
                        .location(requestDto.getLocation())
                        .profileUrl("https://bondyu.s3.ap-northeast-2.amazonaws.com/static/user/%EA%B8%B0%EB%B3%B8%ED%94%84%EB%A1%9C%ED%95%84.png")
                        .build();

        userRepository.save(user);

        return ResponseDto.success(MessageResponseDto.builder()
                .msg("회원가입 성공")
                .build());
    }

    public ResponseDto<?> login(LoginRequestDto requestDto, HttpServletResponse response) {
        User user = isPresentEmail(requestDto.getEmail());
        if (user == null) {
            return ResponseDto.fail(ErrorCode.USER_NOT_FOUND);
        }

        if (!user.validatePassword(passwordEncoder, requestDto.getPassword())) {
            return ResponseDto.fail(ErrorCode.INVALID_USER);
        }

        TokenDto tokenDto = tokenProvider.generateTokenDto(user);
        tokenToHeaders(tokenDto, response);

        return ResponseDto.success(
                LoginResponseDto.builder()
                                .id(user.getId())
                                .nickname(user.getNickname())
                                .token(tokenDto)
                                .build());
    }

    public ResponseDto<?> logout(HttpServletRequest request) {
        if (!tokenProvider.validateToken(request.getHeader("Refresh-Token"))) {
            return ResponseDto.fail(ErrorCode.INVALID_TOKEN);
        }

        User user = tokenProvider.getUserFromAuthentication();
        if (user == null) {
            return ResponseDto.fail(ErrorCode.NOT_LOGIN_STATE);
        }

        return tokenProvider.deleteRefreshToken(user);
    }

    public ResponseDto<?> emailCheck(EmailCheckDto emailCheckDto) {
        User user = isPresentEmail(emailCheckDto.getEmail());
        if (user == null) {
            return ResponseDto.success(MessageResponseDto.builder().msg("사용가능한 이메일입니다.").build());
        }

        return ResponseDto.fail(ErrorCode.DUPLICATED_EMAIL);
    }

    @Transactional
    public ResponseDto<?> editProfile(UserDetailsImpl userDetails,
                                      MultipartFile imageFile,
                                      ProfileEditRequestDto requestDto) throws IOException {
        Long userId = userDetails.getUser().getId();
        User loginUser = userRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        String imageUrl;
        if(imageFile==null){
            imageUrl = loginUser.getProfileUrl();
        } else{
            imageUrl = s3UploadService.s3UploadFile(imageFile,"static/user");
        }

        String nickname;
        if(requestDto == null){
            nickname = loginUser.getNickname();
        } else {
            User user = isPresentNickname(requestDto.getNickname());
            if (user != null) {
                return ResponseDto.fail(ErrorCode.DUPLICATED_NICKNAME);
            }
            nickname = requestDto.getNickname();
        }

        loginUser.edit(imageUrl, nickname);

        return ResponseDto.success(EditProfileResponseDto.builder()
                                                         .id(loginUser.getId())
                                                         .profileUrl(loginUser.getProfileUrl())
                                                         .nickname(loginUser.getNickname())
                                                         .build());
    }

    public ResponseDto<?> certify(CertificationRequestDto requestDto) {

        CertificationNumber certification = certificationNumberRepository.findByEmail(requestDto.getEmail())
                .orElseThrow( () -> new IllegalArgumentException("이메일 오류"));

        if (!requestDto.getCertificationNum().equals(certification.getCertificationNumber())) {
            return ResponseDto.fail(ErrorCode.NUMER_NOT_MATCHED);
        }

        return ResponseDto.success(MessageResponseDto.builder().msg("인증되었습니다.").build());
    }

    public ResponseDto<?> nicknameCheck(NicknameCheckDto nicknameCheckDto) {
        User user = isPresentNickname(nicknameCheckDto.getNickname());
        if (user == null) {
            return ResponseDto.success(MessageResponseDto.builder().msg("사용가능한 닉네임입니다.").build());
        }

        return ResponseDto.fail(ErrorCode.DUPLICATED_NICKNAME);
    }

    @Transactional
    public ResponseDto<?> refreshToken(HttpServletRequest request, HttpServletResponse response){
        if (!tokenProvider.validateToken(request.getHeader("Refresh-Token"))) {
            return ResponseDto.fail(ErrorCode.INVALID_TOKEN);
        }

        String refreshTokenValue = request.getHeader("Refresh-Token");
        RefreshToken refreshToken = refreshTokenRepository.findByRefreshTokenValue(refreshTokenValue).orElseThrow(
                () -> new IllegalArgumentException("리프레쉬 토큰을 찾을 수 없습니다.")
        );
        User user = refreshToken.getUser();

        tokenProvider.deleteRefreshToken(user);
        TokenDto tokenDto = tokenProvider.generateTokenDto(user);
        tokenToHeaders(tokenDto, response);
        return ResponseDto.success(tokenDto);
    }

    @Transactional(readOnly = true)
    public User isPresentEmail(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        return optionalUser.orElse(null);
    }

    @Transactional(readOnly = true)
    public User isPresentNickname(String nickname) {
        Optional<User> optionalUser = userRepository.findByNickname(nickname);
        return optionalUser.orElse(null);
    }

    public void tokenToHeaders(TokenDto tokenDto, HttpServletResponse response) {
        response.addHeader("Authorization", "Bearer " + tokenDto.getAccessToken());
        response.addHeader("Refresh-Token", tokenDto.getRefreshToken());
        response.addHeader("Access-Token-Expire-Time", tokenDto.getAccessTokenExpiresIn().toString());
    }

}
