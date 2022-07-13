package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import io.minio.*;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * author:atGuiGu-mqx
 * date:2022/4/27 10:19
 * 描述：
 **/
@RestController
@RequestMapping("/admin/product/")
@RefreshScope//nacos 配置中心有变化，则会自动更新，自动获取！
public class FileUploadController {

    //  读取配置文件
    @Value("${minio.endpointUrl}")
    private String endpointUrl;

    @Value("${minio.accessKey}")
    public String accessKey;

    @Value("${minio.secreKey}")
    public String secreKey;

    @Value("${minio.bucketName}")
    public String bucketName;

    //  文件上传
    //  获取到上传的文件
    //  /admin/product/fileUpload
    @PostMapping("fileUpload")
    public Result fileUpload(MultipartFile file){
        //  声明一个url
        String url = "";
        try {
            //  创建Minioclient    客户端
            MinioClient minioClient =
                    MinioClient.builder()
                            .endpoint(endpointUrl) //minio的地址
                            .credentials(accessKey, secreKey)//用户名 密码
                            .build();
            //  创建存储桶：
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                // Make a new bucket called 'asiatrip'.
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            } else {
                System.out.println("Bucket 'gmall' already exists.");
            }
            //  文件上传：
            //  定义一个上传之后的文件名称：
            String fileName = System.currentTimeMillis()+ UUID.randomUUID().toString();
            //  Upload known sized input stream.
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(fileName).stream(
                            file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())//文件的类型
                            .build());

            //  接收到返回的url 路径：
            //  http://192.168.200.129:9000/gmall/xxxx;  存储的图片路径
            url = endpointUrl+"/"+bucketName+"/"+fileName;

            System.out.println("url:\t"+url);
            //try cach级别由小到大
        } catch (ErrorResponseException e) {
            e.printStackTrace();
        } catch (InsufficientDataException e) {
            e.printStackTrace();
        } catch (InternalException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidResponseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (XmlParserException e) {
            e.printStackTrace();
        }


        //  返回文件上传的路径。
        return Result.ok(url);
    }

}