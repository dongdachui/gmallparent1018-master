package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * author:atGuiGu-mqx
 * date:2022/5/17 10:11
 * 描述：
 **/
@Controller
// @RestController
@RequestMapping("/api/payment/alipay")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${app_id}")
    private String app_id;

    //  根据订单Id 生成二维码！
    // http://api.gmall.com/api/payment/alipay/submit/47
    @GetMapping("submit/{orderId}")
    @ResponseBody
    public String aliPay(@PathVariable Long orderId){
        //  调用服务层方法.
        String form = null;
        try {
            form = alipayService.createAliPay(orderId);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        //  返回表单并将其输入到页面.
        return form;
    }

    //  http://api.gmall.com/api/payment/alipay/callback/return
    //  编写一个同步回调地址：
    @GetMapping("callback/return")
    public String callbackReturn(){
        //  没有处理订单状态更新。
        //   redirect: http://payment.gmall.com/pay/success.html
        //  重新到web-all 中的某个控制器！
        return "redirect:"+ AlipayConfig.return_order_url;
    }

    //  异步回调：不可见的！
    //  http://kw6zij.natappfree.cc/api/payment/alipay/callback/notify
    //  https: //商家网站通知地址?voucher_detail_list=[{"amount":"0.20","merchantContribute":"0.00","name":"5折券","otherContribute":"0.20","type":"ALIPAY_DISCOUNT_VOUCHER","voucherId":"2016101200073002586200003BQ4"}]&fund_bill_list=[{"amount":"0.80","fundChannel":"ALIPAYACCOUNT"},{"amount":"0.20","fundChannel":"MDISCOUNT"}]&subject=PC网站支付交易&trade_no=2016101221001004580200203978&gmt_create=2016-10-12 21:36:12&notify_type=trade_status_sync&total_amount=1.00&out_trade_no=mobile_rdm862016-10-12213600&invoice_amount=0.80&seller_id=2088201909970555&notify_time=2016-10-12 21:41:23&trade_status=TRADE_SUCCESS&gmt_payment=2016-10-12 21:37:19&receipt_amount=0.80&passback_params=passback_params123&buyer_id=2088102114562585&app_id=2016092101248425&notify_id=7676a2e1e4e737cff30015c4b7b55e3kh6& sign_type=RSA2&buyer_pay_amount=0.80&sign=***&point_amount=0.00
    @PostMapping("callback/notify")
    @ResponseBody
    public String callbackNotify(@RequestParam Map<String, String> paramsMap ){
        //  处理订单状态更新.
        //  Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中
        boolean signVerified = false; //调用SDK验证签名
        try {
            signVerified = AlipaySignature.rsaCheckV1(paramsMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        //  获取回调的out_trade_no;
        String outTradeNo = paramsMap.get("out_trade_no");
        //  获取回调的total_amount
        String totalAmount = paramsMap.get("total_amount");
        //  验证 app_id 是否为该商户本身
        String appId = paramsMap.get("app_id");
        //  获取到交易状态：
        String tradeStatus = paramsMap.get("trade_status");
        //  获取到notify_id;
        String notifyId = paramsMap.get("notify_id");

        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            //  outTradeNo 查询数据库如果能找到数据，则说明out_trade_no 是商户的订单号 payment_info
            //  支付的时，金额固定的0.01 new BigDecimal("0.01").compareTo(new BigDecimal(totalAmount))!=0
            //  paymentInfoQuery.getTotalAmount().compareTo(new BigDecimal(totalAmount))!=0
            PaymentInfo paymentInfoQuery = this.paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
            if (paymentInfoQuery==null || new BigDecimal("0.01").compareTo(new BigDecimal(totalAmount))!=0
            || !appId.equals(app_id)){
                return "failure";
            }
            //  将 notify_id 放入缓存 setnx ,setex  判断这个key 是否存在，如果不存在，执行。如果存在，则返回。
            //  24*60+22
            Boolean result = this.redisTemplate.opsForValue().setIfAbsent(notifyId, notifyId, 1462 * 60, TimeUnit.SECONDS);
            if (!result){
                //  说明缓存中有数据，说明有回调信息.
                return "failure";
            }
            //  判断交易状态.
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)){
                //  更新交易记录的状态.trade_no payment_status callback_time callback_content
                this.paymentService.paySuccess(outTradeNo,PaymentType.ALIPAY.name(),paramsMap);
                return "success";
            }
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        //  默认返回failure
        return "failure";
    }

    //  退款接口：
    @GetMapping("refund/{orderId}")
    @ResponseBody
    public Boolean refund(@PathVariable Long orderId){
        //  调用退款接口.
        Boolean flag = this.alipayService.refund(orderId);
        //  默认返回.
        return flag;
    }

    //  关闭支付宝交易记录.
    @GetMapping("closePay/{orderId}")
    @ResponseBody
    public Boolean closeAliPay(@PathVariable Long orderId){
        //  调用关闭接口.
        Boolean flag = this.alipayService.closeAliPay(orderId);
        return flag;
    }

    //  查询交易记录接口：
    @GetMapping("checkPayment/{orderId}")
    @ResponseBody
    public Boolean checkPayment(@PathVariable Long orderId){
        //  调用关闭接口.
        Boolean flag = this.alipayService.checkPayment(orderId);
        return flag;
    }

    //  添加查询paymentInfo 记录.
    @GetMapping("getPaymentInfo/{outTradeNo}")
    @ResponseBody
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo){
        //  调用服务层方法.
        PaymentInfo paymentInfo = this.paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
        if (paymentInfo!=null){
            return paymentInfo;
        }
        return null;
    }



}