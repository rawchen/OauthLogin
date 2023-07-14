package com.rawchen.oauthlogin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author RawChen
 * @date 2023-07-12 13:38
 */
@Data
@Builder
@Accessors(chain = true)
@TableName("user")
public class User {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String userId;

	private String img;

	private String openId;

	private String name;
}
