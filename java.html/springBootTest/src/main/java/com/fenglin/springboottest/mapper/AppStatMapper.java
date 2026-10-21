package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.AppStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 站点统计计数 Mapper（MyBatis-Plus）
 */
@Mapper
public interface AppStatMapper extends BaseMapper<AppStat> {

	/**
	 * 计数 +1；若统计项不存在则创建（初值 1）
	 */
	@Update("INSERT INTO app_stat (`code`, `cnt`) VALUES (#{code}, 1) " +
			"ON DUPLICATE KEY UPDATE `cnt` = `cnt` + 1")
	int incrementOrCreate(@Param("code") String code);

	/**
	 * 读取某统计项计数
	 */
	@Select("SELECT `cnt` FROM app_stat WHERE `code` = #{code}")
	Long selectCnt(@Param("code") String code);
}
