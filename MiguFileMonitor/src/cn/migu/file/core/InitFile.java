/**
 * 
 */
package cn.migu.file.core;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * @author gus97 2016年11月29日 上午9:40:53
 */
public class InitFile {

	static private Logger logger = LoggerFactory.getLogger(InitFile.class);

	static private Map<Pattern, Pace> map = new HashMap<Pattern, Pace>();

	@SuppressWarnings("static-access")
	public static void initFileMonitor(Map<String, List<Pace>> rp, String aid, String sid, String jid) {

		for (Entry<String, List<Pace>> entry1 : rp.entrySet()) {

			for (Pace pace : entry1.getValue()) {

				map.put(pace.getPattern(), pace);
			}
		}

		// 读取数据中2个小时前的所有文件

		// 远程调用服务QUERY_FLUME_LOGS
		Gson gson = new Gson();
		Map<String, String> mapOj = new HashMap<String, String>();
		mapOj.put("sql", SqlConstant.QUERY_FLUME_LOGS);
		String resultJson = HttpPostUtil.post(SqlConstant.QUERYFORLISTURL, mapOj);
		mapOj.put("param", JSONObject.toJSONString(new Object[] { aid, sid, jid,SqlConstant.sqlRollTime }));
		resultJson = HttpPostUtil.post(SqlConstant.QUERYFORLISTURL, mapOj);
		JSONObject resObj = JSONObject.parseObject(resultJson);
		BaseResponse response = resObj.parseObject(resultJson, BaseResponse.class);
		resObj = JSONObject.parseObject(resultJson);
		response = resObj.parseObject(resultJson, BaseResponse.class);
		List<Map<String, Object>> resQueryFL = null;
		// 列表为空时返回[]
		String arrStr = response.getResponse().getContent();
		if (!"[]".equals(arrStr)) {

			resQueryFL = gson.fromJson(arrStr, new TypeToken<List<Map<String, Object>>>() {
			}.getType());
		} else {
			resQueryFL = null;
		}

		Set<String> collectFiles = new HashSet<String>();
		
		

		Double rt = Math.ceil((System.currentTimeMillis() - SqlConstant.rollTime) / 1000 / 60 / 60D);
		// 查询所有已收集过的文件
		if (resQueryFL != null && resQueryFL.size() != 0) {
			for (Map<String, Object> map : resQueryFL) {
				String fname = (String) map.get("FNAME");

				collectFiles.add(StringUtils.replace(fname, "\\", "/"));
			}
			logger.info("----------------------->\t" + rt + "小时前数据库中文件数量为: " + resQueryFL.size());
		} else {
			logger.info("----------------------->\t" + rt + "小时前数据库中文件数量为: 0");
		}

		System.out.println(collectFiles+"===================");
		
		for (Entry<String, List<Pace>> entry2 : rp.entrySet()) {

			logger.info("----------------------->\t开始扫描原始路径[" + entry2.getKey() + "],自动回退 " + rt + " 小时");

			long backTime = SqlConstant.rollTime;

			Collection<File> listFiles = FileUtils.listFiles(new File(entry2.getKey()),
					FileFilterUtils.ageFileFilter(backTime, false), DirectoryFileFilter.INSTANCE);

			Set<String> fileSet = new HashSet<String>();

			for (File file : listFiles) {

				fileSet.add(file.getPath().replace("\\", "/"));
			}

			logger.info("----------------------->\t原始路径 [" + entry2.getKey() + "] 当前时间" + rt + "小时内产生文件总数: "
					+ fileSet.size());
			
			System.out.println(fileSet+"$$$$$$$$$$$$$$"+collectFiles);

			fileSet.removeAll(collectFiles);

			logger.info("----------------------->\t原始路径 [" + entry2.getKey() + "] " + rt + "小时差异文件: " + fileSet);

			logger.info("----------------------->\t原始路径 [" + entry2.getKey() + "] 开始文件入列!");

			for (String fn : fileSet) {

				try {
					int bl = 0;

					// 统一路径下的多个表达式，不同的表达式写不同的采集路径
					for (Entry<Pattern, Pace> entry : map.entrySet()) {

						Matcher matcher = entry.getKey().matcher(fn.split("/")[fn.split("/").length - 1]);

						if (matcher.matches()) {

							bl++;

							BasicFileAttributes attributes = Files.readAttributes(Paths.get(fn),
									BasicFileAttributes.class);

							String uniSuffix = "." + UUID.randomUUID().toString();

							Pace p = entry.getValue();

							BlockingQueue4Files.bqf.get(entry.getKey())
									.add(new BlockingQueneFileInfo(p.getAid(), p.getSid(), p.getJid(), p.getOid(), fn,
											fn.split("/")[fn.split("/").length - 1] + uniSuffix, attributes.size(),
											CodeDetectUtil.fileLength(fn), p.getCollectPath(),
											FileWriteUtil.getLongDateString(attributes.creationTime().toMillis()),
											"UTF-8"));
							logger.info("----------------------->\t源文件[" + fn + "] 成功写入 [" + entry.getKey() + "] 队列！");

							break;
						}
					}
					if (bl == 0) {
						logger.warn("----------------------->\t[" + fn + "] 遍历所有表达式,没有任何匹配!");
					}

				} catch (Exception e) {

					logger.error("ERROR: " + fn + "\t----------------------->\t", e);

				}
			}
		}
	}
}
