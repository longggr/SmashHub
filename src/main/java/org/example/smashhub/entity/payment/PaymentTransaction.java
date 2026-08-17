package org.example.smashhub.entity.payment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.example.smashhub.entity.order.Order;
import org.example.smashhub.shared.enums.IpnStatus;
import org.example.smashhub.shared.persistence.BaseEntity;

/**
 * Cho phép nhiều PaymentTransaction cùng order_id (VNPay retry flow) - có chủ đích.
 * status giữ nguyên String (VARCHAR(255), không ràng buộc enum) theo quyết định
 * chưa tối ưu hóa cột này trong conversation.
 * response_code/transaction_status/transaction_no/bank_code là field riêng của VNPay -
 * nếu sau này thêm cổng thanh toán khác, cân nhắc gộp thành 1 cột JSON provider_metadata.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, exclude = {"order"})
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "payment_transaction")
public class PaymentTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    Order order;
    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "txn_ref", nullable = false, unique = true)
    private String txnRef;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "response_code", length = 10)
    private String responseCode;

    @Column(name = "transaction_status", length = 10)
    private String transactionStatus;

    @Column(name = "transaction_no", length = 20)
    private String transactionNo;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Column(name = "pay_date")
    private LocalDateTime payDate;

    @Column(name = "expire_date")
    private LocalDateTime expireDate;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "ipn_processed", nullable = false)
    private IpnStatus ipnProcessed;

    @Lob
    @Column(name = "raw_request_payload")
    private String rawRequestPayload;

    @Lob
    @Column(name = "raw_response_payload")
    private String rawResponsePayload;

    @Column(name = "failure_reason")
    private String failureReason;




}
