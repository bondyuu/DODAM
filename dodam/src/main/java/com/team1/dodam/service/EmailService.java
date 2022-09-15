package com.team1.dodam.service;

import com.team1.dodam.domain.CertificationNumber;
import com.team1.dodam.dto.MailDto;
import com.team1.dodam.dto.request.MailRequestDto;
import com.team1.dodam.dto.response.ResponseDto;
import com.team1.dodam.repository.CertificationNumberRepository;
import lombok.AllArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@AllArgsConstructor
public class EmailService {

    private final CertificationNumberRepository certificationNumberRepository;
    private JavaMailSender emailSender;

    @Transactional
    public ResponseDto<?> send(MailRequestDto requestDto) {

        int certificationNumber = makeRandomNumber();
        String title = "회원가입 인증 메일입니다.";
        String emailFrom = "team3mailsender@gmail.com";

        certificationNumberRepository.deleteByEmail(requestDto.getAddress());


        CertificationNumber certification = certificationNumberRepository.save(CertificationNumber.builder()
                                                              .email(requestDto.getAddress())
                                                              .certificationNumber((long)certificationNumber)
                                                              .build());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailFrom);
        message.setTo(certification.getEmail());
        message.setSubject(title);
        message.setText(Long.toString(certification.getCertificationNumber()));
        emailSender.send(message);
        return ResponseDto.success("success");
    }

    public int makeRandomNumber() {
        Random r = new Random();
        return r.nextInt(899999)+100000;
    }
}  
