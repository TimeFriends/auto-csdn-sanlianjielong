package top.chen.fansback.common.spider.csdn;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import top.chen.fansback.common.BackProperties;
import top.chen.fansback.common.cmd.CsdnRequest;
import top.chen.fansback.common.cmd.RequestUtil;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 1. 定时多久扫描我的消息栏（前提登录），看是否有点赞、关注的信息
 * 2. 有的话，则顺着用户id去查看用户的第一篇文章，给他返回一个关注、点赞、评论，记录一下到本地
 * 3. 如果已经返过了，那就不用返了
 * 4. 记录的逻辑是：关注：关注人-被关注人唯一；点赞人-文章id唯一、收藏人-文章id唯一；
 *
 */
@Slf4j
public class BackFansSpider {
	static boolean FLAG = true;

	/**
	 * 根据连接给文章评论 收藏 点赞
	 */
	@SneakyThrows
	static void jieLongSanLian(String URL){
		List<String> urlStrList = huZanURL(URL);
		if (urlStrList != null && urlStrList.size() > 0) {
			//评论
			for (String url : urlStrList) {
				String urlHtml = RequestUtil.doGetStr(url);
				Pattern urlHtmlCompile = Pattern.compile("(canonical\" href=\")(.*?)(\"/>)");
				Matcher urlHtmlMatcher = urlHtmlCompile.matcher(urlHtml);
				while (urlHtmlMatcher.find()) {
					urlHtml =  urlHtmlMatcher.group();
				}
				url = urlHtml.split("canonical\" href=\"|\"/>")[1];
				Map<String, String> queryParamByUrl = CsdnRequest.queryParamByUrl(url);
				if (CsdnRequest.postComment(RandomUtil.randomEle(BackProperties.replayCommentArr, BackProperties.replayCommentArr.length - 1), url)) {
					log.info("评论已处理完成！！！文章为连接{}", url);
				}else{
					log.warn("评论异常！！！程序结束! 异常的文章链接为", url);
				}
				//点赞
				boolean b = CsdnRequest.postLikeArticle(url);
				if (b) {
					log.info("点赞已处理完成！！！文章为连接{}", url);
				}else{
					log.warn("点赞异常！！！程序结束! 异常的文章链接为", url);
				}
				//收藏
				boolean success = CsdnRequest.postAddFav(url, String.valueOf(queryParamByUrl.get("userName")), String.valueOf(queryParamByUrl.get("title")),String.valueOf(queryParamByUrl.get("description")));
				if (success) {
					log.info("收藏已处理完成！！！文章为连接：{}", url);
				}else{
					log.warn("收藏异常！！！程序结束! 异常的文章链接为", url);
				}
			}
			log.info("--------------------------------------------------------接龙处理完成--------------------------------------------------------");
			FLAG = false;
		}else{
			log.info("--------------------接龙地址解析错误--------------------");
			FLAG = false;
		}
	}

	/**
	 * 接龙的连接地址分隔
	 *
	 * @return
	 */
	private static List<String> huZanURL(String URL) {
		Pattern pattern = Pattern.compile("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");
		Matcher matcher = pattern.matcher(URL);
		List<String> urlArr = new ArrayList<>();
		while (matcher.find()) {
			urlArr.add(matcher.group());
		}
		log.info("截取后的连接地址{}",JSON.toJSONString(urlArr));
		return urlArr;
	}

	@SneakyThrows
	public static void main(String[] args) {
		while (true) {
			try {
                if (!FLAG) {
                    return;
                }
				//添加接龙内容
				jieLongSanLian("");

//				 eg :jieLongSanLian("17. Lee\n" +
//						" http://t.csdn.cn/hN1Lx诚信四连，看到就回\n" +
//						"18. CSDN-蜡笔雏田学代码 http://t.csdn.cn/F2YfH 优质互奶️\n" +
//						"19. http://t.csdn.cn/aNPp6 新发文章，三连必回！\n" +
//						"20. promise https://blog.csdn.net/m0_71485750/article/details/126193633  互三互关\n" +
//						"21. 渟 http://t.csdn.cn/lxE3v 原力7级在线回\n" +
//						"22. 零 http://t.csdn.cn/5NsU1 互三互关");
			} catch (Exception e) {
				log.error("发送异常：", e);
			}
		}
	}
}
