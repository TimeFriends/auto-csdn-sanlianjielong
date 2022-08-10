package top.chen.fansback.common.cmd;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import top.chen.fansback.common.spider.csdn.dto.fav.FavAddBody;

import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



@Slf4j
public class CsdnRequest {

	public static String OWNER = "";//自己的name配置到此
	public static int MESSAGE_PAGE_SIZE = 15;
	public static boolean LIKE_LIMIT = false;



	//CSDN_COOKIE 字符串
	static String CSDN_COOKIE="";

	/**
	 * 评论限流 21s，每分钟只能评论三条
	 */
	static int COMMENT_SLEEP = 21;

	//收藏夹的defaultFavFoldId 如果是 1 需要写成1L
	static long defaultFavFoldId = 1L;
	
	// 收藏操作
	public static boolean postAddFav(String url, String author, String title, String desc) {
		FavAddBody addBody = new FavAddBody();
		addBody.setUrl(url);
		addBody.setSource("blog");
		addBody.setSourceId(Long.parseLong(getArticleId(url)));
		addBody.setAuthor(author);
		addBody.setTitle(title);
		addBody.setDescription(desc);
		addBody.setFromType("PC");
		addBody.setUsername(OWNER);
		addBody.setFolderId(defaultFavFoldId);
		addBody.setNewFolderName("");
		HttpRequest request = HttpUtil.createPost("https://mp-action.csdn.net/interact/wrapper/pc/favorite/v1/api/addFavorite")
				.body(JSON.toJSONString(addBody));
		addHead(request, "accept: */*\n" +
				"accept-encoding: gzip, deflate, br\n" +
				"accept-language: zh-CN,zh;q=0.9,en;q=0.8,zh-TW;q=0.7,zh-HK;q=0.6\n" +
				"cache-control: no-cache\n" +
				"content-length: 418\n" +
				"content-type: application/json\n" +
				"origin: https://blog.csdn.net\n" +
				"pragma: no-cache\n" +
				"referer: " + url + "\n" +
				"sec-ch-ua: \".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"\n" +
				"sec-ch-ua-mobile: ?0\n" +
				"sec-ch-ua-platform: \"Windows\"\n" +
				"sec-fetch-dest: empty\n" +
				"sec-fetch-mode: cors\n" +
				"sec-fetch-site: same-site\n" +
				"user-agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");
		HttpResponse response = request.execute();
		log.info("收藏操作：{},{},{}", url, author, response.body());
		return response.isOk() && response.body().contains("success");
	}

	/**
	 * 给文章评论
	 */
	@SneakyThrows
	public static boolean postComment(String comment, String url) {
		HttpRequest post = HttpUtil.createPost("https://blog.csdn.net/phoenix/web/v1/comment/submit");
		if (StrUtil.isEmpty(url)) {
			log.warn("url 为空 ");
			return false;
		}
		String article = getArticleId(url);
		post.form("content", comment);
		post.form("articleId", article);
		addHead(post, "accept: application/json, text/javascript, */*; q=0.01\n" +
				"accept-encoding: gzip, deflate, br\n" +
				"accept-language: zh-CN,zh;q=0.9,en;q=0.8,zh-TW;q=0.7,zh-HK;q=0.6\n" +
				"cache-control: no-cache\n" +
				"content-length: 211\n" +
				"content-type: application/x-www-form-urlencoded; charset=UTF-8\n" +
				"origin: https://blog.csdn.net\n" +
				"pragma: no-cache\n" +
				"referer: " + url + "\n" +
				"sec-ch-ua: \".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"\n" +
				"sec-ch-ua-mobile: ?0\n" +
				"sec-ch-ua-platform: \"Windows\"\n" +
				"sec-fetch-dest: empty\n" +
				"sec-fetch-mode: cors\n" +
				"sec-fetch-site: same-origin\n" +
				"user-agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36\n" +
				"x-requested-with: XMLHttpRequest\n" +
				"x-tingyun-id: im-pGljNfnc;r=27412521");
		HttpResponse httpResponse = post.executeAsync();
		log.info("[{}}]评论[{}]返回：{}", comment, url, httpResponse.body());
		if (httpResponse.body().contains("您的账号已被禁言")) {
			log.info("您的账号已被禁言");
			return false;
		}
		TimeUnit.SECONDS.sleep(COMMENT_SLEEP);
		return httpResponse.isOk() && httpResponse.body().contains("success");
	}

	private static String getArticleId(String url) {
		String[] split = url.split("\\?")[0].split("/");
		return split[split.length - 1];
	}

	static void addHead(HttpRequest request, String defaultString) {
		try {
			String[] split = defaultString.split("\n");
			for (String s1 : split) {
				String[] split1 = s1.split(": ");
				request.header(split1[0], split1[1]);
			}
			request.header("cookie", CSDN_COOKIE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	/**
	 * 点赞文章
	 */
	public static boolean postLikeArticle(String url) {
		String articleId = getArticleId(url);
		URL toUrlForHttp = URLUtil.toUrlForHttp(url);
		HttpRequest request = HttpUtil.createPost("https://blog.csdn.net//phoenix/web/v1/article/like")
				.form("articleId", articleId);
		addHead(request, "accept: application/json, text/javascript, */*; q=0.01\n" +
				"accept-encoding: gzip, deflate, br\n" +
				"accept-language: zh-CN,zh;q=0.9,en;q=0.8,zh-TW;q=0.7,zh-HK;q=0.6\n" +
				"cache-control: no-cache\n" +
				"content-length: 19\n" +
				"content-type: application/x-www-form-urlencoded; charset=UTF-8\n" +
				"origin: " + String.format("%s://%s", toUrlForHttp.getProtocol(), toUrlForHttp.getHost()) + "\n" +
				"pragma: no-cache\n" +
				"referer: " + url + "\n" +
				"sec-ch-ua: \".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"\n" +
				"sec-ch-ua-mobile: ?0\n" +
				"sec-ch-ua-platform: \"Windows\"\n" +
				"sec-fetch-dest: empty\n" +
				"sec-fetch-mode: cors\n" +
				"sec-fetch-site: same-site\n" +
				"user-agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");
		HttpResponse execute = request.execute();
		log.info("点赞操作 {} , {}", url, execute.body());
		if (execute.body().contains("已达上限")) {
			LIKE_LIMIT = true;
		}
		return execute.isOk() && execute.body().contains("success") && execute.body().contains("status\":true");

	}
	

	/**
	 * 根据文章连接获取 userName,title,description
	 */
	public static Map<String,String> queryParamByUrl(String URL) {
		HttpRequest request = HttpUtil.createGet(URL);
		addHead(request,
				"accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9\n" +
						"accept-encoding: gzip, deflate, br\n" +
						"accept-language: zh-CN,zh;q=0.9\n" +
						"sec-ch-ua: \".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"\n" +
						"sec-ch-ua-mobile: ?0\n" +
						"sec-ch-ua-platform: \"Windows\"\n" +
						"sec-fetch-dest: document\n" +
						"sec-fetch-mode: navigate\n" +
						"sec-fetch-site: none\n" +
						"sec-fetch-user: ?1\n" +
						"upgrade-insecure-requests: 1\n" +
						"user-agent: Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");
		String body = request.execute().body();
		String userName = "" ;
		String title = "";
		String description = "";
		String url = "";
		Map<String,String> map = new HashMap<>();

		Pattern userNameCompile = Pattern.compile("(username =  \")(.*?)(\")");
		Matcher userNameMatcher = userNameCompile.matcher(body);
		while (userNameMatcher.find()) {
			userName = userNameMatcher.group();
		}
		userName = userName.split("\"")[1];
		Pattern titleCompile = Pattern.compile("(title\":\")(.*?)(\"})");
		Matcher titleMatcher = titleCompile.matcher(body);
		while (titleMatcher.find()) {
			title =  titleMatcher.group();
		}
		title = title.split("\"")[2];
		Pattern descriptionCompile = Pattern.compile("(description\" content=\")(.*?)(\">)");
		Matcher descriptionMatcher = descriptionCompile.matcher(body);
		while (descriptionMatcher.find()) {
			description =  descriptionMatcher.group();
		}
		description = description.split("description\" content=\"|\">")[1];
		Pattern urlCompile = Pattern.compile("(\\?from=)(.*?)(\")");
		Matcher urlMatcher = urlCompile.matcher(body);
		while (urlMatcher.find()) {
			url =  urlMatcher.group();
		}
		url = url.split("\\?from=|\"")[1];
		log.info("根据文章连接获取 userName{},title{},description{},url{}",userName,title,description,url);
		map.put("userName", userName);
		map.put("title",title );
		map.put("description", description);
		map.put("url", url);
		return map;
	}
}
