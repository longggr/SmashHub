package org.example.smashhub.entity.order;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.smashhub.entity.User;
import org.example.smashhub.entity.location.District;
import org.example.smashhub.entity.location.Province;
import org.example.smashhub.entity.location.Ward;
import org.example.smashhub.shared.enums.OrderStatus;
import org.example.smashhub.shared.enums.PaymentMethod;
import org.example.smashhub.shared.enums.ShippingMethod;
import org.example.smashhub.shared.persistence.BaseEntity;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, exclude = {"user"})
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "orders")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 20)
    OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_method", nullable = false, length = 15)
    ShippingMethod shippingMethod;

    @Column(name = "shipping_recipient_name",nullable = false)
    String shippingRecipientName;

    @Column(name = "shipping_phone", nullable = false,length = 20)
    String shippingPhone;

    @Column(name = "shipping_line1", nullable = false)
    String shippingLine1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_province_id", nullable = false)
    Province shippingProvince;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_district_id", nullable = false)
    District shippingDistrict;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_ward_code", nullable = false)
    Ward shippingWard;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    BigDecimal subtotal;

    @Column(name = "shipping_cost", nullable = false, precision = 15, scale = 2)
    BigDecimal shippingCost;

    @Column(name = "discount", nullable = false, precision = 15, scale = 2)
    BigDecimal discount;

    @Column(name = "total", nullable = false, precision = 15, scale = 2)
    BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    PaymentMethod paymentMethod;







}
