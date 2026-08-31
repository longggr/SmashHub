package org.example.smashhub.notification.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.smashhub.exception.AppException;
import org.example.smashhub.exception.ErrorCode;
import org.example.smashhub.notification.service.EmailService;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailServiceImpl implements EmailService {

    JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("[SmashHub] Ma xac thuc tai khoan cua ban");
            helper.setText(buildOtpEmailContent(otp), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Send OTP email failed for {}", toEmail, e);
            throw new AppException(ErrorCode.MAIL_SEND_FAILED);
        }
    }

    @Override
    public void sendPasswordResetOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("[SmashHub] Ma dat lai mat khau cua ban");
            helper.setText(buildPasswordResetEmailContent(otp), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Send password reset email failed for {}", toEmail, e);
            throw new AppException(ErrorCode.MAIL_SEND_FAILED);
        }
    }

    private String buildOtpEmailContent(String otp) {
        return """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 480px; margin: 0 auto; background-color: #ffffff; padding: 32px; border-radius: 12px; border: 1px solid #e0e0e0; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);">
                          <div style="text-align: center; margin-bottom: 24px;">
                              <h2 style="color: #1a73e8; font-size: 24px; margin: 0; font-weight: 700;">Xác thực tài khoản SmashHub</h2>
                          </div>
                
                          <p style="color: #333333; font-size: 15px; line-height: 1.5; margin-bottom: 16px;">
                              Xin chào,
                          </p>
                          <p style="color: #333333; font-size: 15px; line-height: 1.5; margin-bottom: 24px;">
                              Bạn vừa yêu cầu mã xác thực cho tài khoản SmashHub của mình. Dưới đây là mã OTP của bạn:
                          </p>
                
                          <div style="background-color: #f8f9fa; border: 1px dashed #1a73e8; border-radius: 8px; padding: 16px; text-align: center; margin-bottom: 24px;">
                              <span style="font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #1a73e8;">%s</span>
                          </div>
                
                          <p style="color: #666666; font-size: 14px; line-height: 1.5; margin-bottom: 8px;">
                              Mã này có hiệu lực trong vòng <strong>5 phút</strong>. Vui lòng không chia sẻ mã này cho bất kỳ ai để đảm bảo an toàn tài khoản.
                          </p>
                
                          <hr style="border: none; border-top: 1px solid #eeeeee; margin: 24px 0;">
                
                          <p style="color: #999999; font-size: 12px; text-align: center; margin: 0;">
                              Trân trọng,<br>
                              <strong>Đội ngũ SmashHub</strong>
                          </p>
                      </div>
                """.formatted(otp);
    }

    private String buildPasswordResetEmailContent(String otp) {
        return """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 480px; margin: 0 auto; background-color: #ffffff; padding: 32px; border-radius: 12px; border: 1px solid #e0e0e0; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);">
                          <div style="text-align: center; margin-bottom: 24px;">
                              <h2 style="color: #d93025; font-size: 24px; margin: 0; font-weight: 700;">Yêu cầu đặt lại mật khẩu</h2>
                          </div>
                
                          <p style="color: #333333; font-size: 15px; line-height: 1.5; margin-bottom: 16px;">
                              Xin chào,
                          </p>
                          <p style="color: #333333; font-size: 15px; line-height: 1.5; margin-bottom: 24px;">
                              Ai đó (hy vọng là bạn) vừa yêu cầu đặt lại mật khẩu cho tài khoản SmashHub này. Mã xác nhận của bạn là:
                          </p>
                
                          <div style="background-color: #fdf3f2; border: 1px dashed #d93025; border-radius: 8px; padding: 16px; text-align: center; margin-bottom: 24px;">
                              <span style="font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #d93025;">%s</span>
                          </div>
                
                          <p style="color: #666666; font-size: 14px; line-height: 1.5; margin-bottom: 8px;">
                              Mã này có hiệu lực trong vòng <strong>5 phút</strong>. Nếu bạn không yêu cầu điều này, vui lòng bỏ qua email và không chia sẻ mã cho bất kỳ ai.
                          </p>
                
                          <hr style="border: none; border-top: 1px solid #eeeeee; margin: 24px 0;">
                
                          <p style="color: #999999; font-size: 12px; text-align: center; margin: 0;">
                              Trân trọng,<br>
                              <strong>Đội ngũ SmashHub</strong>
                          </p>
                      </div>
                """.formatted(otp);
    }
}
