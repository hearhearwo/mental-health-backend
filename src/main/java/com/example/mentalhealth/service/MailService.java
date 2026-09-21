package com.example.mentalhealth.service;

import com.example.mentalhealth.common.BusinessException;
import com.example.mentalhealth.common.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 发送验证码邮件。
 *
 * 没配 SMTP 时不报错，而是把验证码打到控制台日志（开发模式），
 * 这样本地没有邮箱服务也能把注册流程跑通。
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String host;
    private final String from;

    public MailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                       @Value("${spring.mail.host:}") String host,
                       @Value("${spring.mail.username:}") String from) {
        this.mailSenderProvider = mailSenderProvider;
        this.host = host;
        this.from = from;
    }

    public void sendCode(String email, String code) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();

        // 不能只看 bean 是否存在。application.yml 里声明了 spring.mail.host，
        // 即使环境变量没配、值是空串，Spring Boot 的 MailSenderCondition 也认为
        // 属性「存在」从而创建了 JavaMailSender，拿空账号去认证只会抛
        // AuthenticationFailedException。所以必须判断 host 本身有没有值。
        if (sender == null || !StringUtils.hasText(host)) {
            log.warn("【开发模式】未配置 SMTP，验证码未真正发送。email={} code={}。"
                    + "设置 SMTP_HOST / SMTP_USERNAME / SMTP_PASSWORD 后即会真实发信。", email, code);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(StringUtils.hasText(from) ? from : "noreply@mental-health.local");
            message.setTo(email);
            message.setSubject("【心理健康 AI 助手】注册验证码");
            message.setText("你的注册验证码是：" + code + "\n\n"
                    + "有效期 5 分钟，请勿泄露给他人。\n"
                    + "若非本人操作，请忽略本邮件。");
            sender.send(message);
            log.info("验证码邮件已发送至 {}", email);
        } catch (Exception e) {
            // 包装成可读错误，否则前端只会看到「服务器内部错误」
            log.error("验证码邮件发送失败，email={}", email, e);
            throw new BusinessException(ResultCode.ERROR, "验证码邮件发送失败，请稍后重试或联系管理员");
        }
    }
}
