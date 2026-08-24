package org.example.smashhub.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.smashhub.exception.AppException;
import org.example.smashhub.exception.ErrorCode;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailService {
    JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp){
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("[SmashHub] Ma xac thuc tai khoan cua ban");
            helper.setText(buildOtpEmailContent(otp), true);
            mailSender.send(message);
        }catch (MessagingException e){
            log.error("Send OTP email failed for {}", toEmail, e);
            throw new AppException(ErrorCode.MAIL_SEND_FAILED);
        }
    }
    private String buildOtpEmailContent(String otp) {
        return """
                <div style="font-family: Arial, sans-serif; max-width:480px; margin:auto;">
                    <h2 style="color:#1a73e8;">Xac thuc tai khoan SmashHub</h2>
                    <p>Ma OTP cua ban la:</p>
                    <p style="font-size:28px; font-weight:bold; letter-spacing:4px; color:#1a1a1a;">%s</p>
                    <p>Ma co hieu luc trong 5 phut. Vui long khong chia se ma nay cho bat ky ai.</p>
                </div>
                """.formatted(otp);
    }
}
