package com.yami.shop.api.controller;

import com.yami.shop.bean.dto.ImageDto;
import com.yami.shop.bean.model.Image;
import com.yami.shop.common.response.ServerResponseEntity;
import com.yami.shop.dao.ImgMapper;
import com.yami.shop.service.ImgService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;


@RestController
@RequestMapping("/Img")
public class ImgDetectController {

    public static final  String PAYMENT_URL = "http://localhost:8090";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ImgService imgService;
    @Autowired
    private ImgMapper imageMapper;


    @PostMapping("/detect/{userid}")
    public ResponseEntity<Image> getDetectImg(@RequestParam("file") MultipartFile file, @PathVariable String userid) throws IOException {
        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };
        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("file", fileResource);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        String pythonServiceUrl = PAYMENT_URL+"/api/img";
        ResponseEntity<Image> response = restTemplate.postForEntity(pythonServiceUrl, requestEntity,Image.class);
        Image image = response.getBody();
        LocalDateTime now = LocalDateTime.now();
        if (image != null) {
            image.setUserId(userid);
            image.setTime(now);
        }
        if (response.getStatusCode().is2xxSuccessful()) {
            imgService.saveImage(response.getBody());
            return new ResponseEntity<>(image, HttpStatus.OK);
        } else {
            System.out.println("Failed to call Python API");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/history")
    public ServerResponseEntity<List<Image>> getHistory(@RequestParam String userId){
        System.out.println(userId);
        System.out.println(imageMapper.selectList(null));
        List<Image>imageList = imgService.getHistory(userId);
        System.out.println(imageList);
        return ServerResponseEntity.success(imageList);
    }


/*
* 建表语句
* CREATE TABLE History(
	user_id VARCHAR(255) NOT NULL,
	image_id VARCHAR(255) NOT NULL,
	file_name VARCHAR(255) NOT NULL,
	image_data LONGBLOB NOT NULL,
	search_time TIMESTAMP NOT NULL
);
*/
}
