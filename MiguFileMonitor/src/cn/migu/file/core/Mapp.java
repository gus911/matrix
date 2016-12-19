package cn.migu.file.core;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Mapp {

	public static Object lock = new Object();

	static private Logger logger = LoggerFactory.getLogger(Mapp.class);

	@SuppressWarnings("resource")
	public static void main(final String[] args) {

		if (args == null || args.length < 8) {

			logger.error("args length less than 8. input: app_id server_id jar_id deal_user port...");

			System.exit(0);

			return;
		}

		SqlConstant.QUERYFORLISTURL = args[5];

		SqlConstant.EXECUTEURL = args[6];

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

		if (args[7] != null && StringUtils.isNumeric(args[7])) {

			Date date;
			try {
				date = sdf.parse(args[7]);
				SqlConstant.rollTime = date.getTime();
			} catch (ParseException e) {
				e.printStackTrace();
				return;
			}
		}

		Date date;
		try {
			date = sdf.parse(args[7]);
			SqlConstant.sqlRollTime = (int) Math.ceil((System.currentTimeMillis() - date.getTime()) / 1000 / 60 / 60D);
		} catch (ParseException e2) {
			e2.printStackTrace();
		}

		int port = Integer.valueOf(args[4]);

		try {
			new ServerSocket(port);

		} catch (IOException e1) {

			logger.error(e1.getMessage());

			return;
		}

		String jvmName = ManagementFactory.getRuntimeMXBean().getName();
		logger.info("----------------------->\tpid:" + jvmName.split("@")[0]);
		logger.info("----------------------->\tServer is listening on port:" + port);

		Map<String, String> mapOj = new HashMap<String, String>();
		mapOj.put("sql", SqlConstant.QUERY_FLUME_FILES);

		mapOj.put("param", JSONObject.toJSONString(new Object[] { args[0], args[1], args[2] }));
		String resultJson = HttpPostUtil.post(SqlConstant.QUERYFORLISTURL, mapOj);

		BaseResponse response = JSON.parseObject(resultJson, BaseResponse.class);

		// 列表为空时返回[]
		String arrStr = response.getResponse().getContent();

		final List<Map<String, Object>> res;

		Gson gson = new Gson();

		if (!"[]".equals(arrStr)) {
			res = gson.fromJson(arrStr, new TypeToken<List<Map<String, Object>>>() {
			}.getType());
		} else {
			res = null;
		}

		if (res == null || res.size() == 0) {

			logger.error("data query is null");

			System.exit(0);
		}

		final Map<String, List<Pace>> rp = new HashMap<String, List<Pace>>();

		List<Pace> lp = new ArrayList<>();

		String initPathTmp = "";

		for (Map<String, Object> map : res) {

			String code = (String) map.get("CODE");
			Pattern pattern = null;

			if (code != null) {
				pattern = Pattern.compile(code);
			} else {
				logger.error("----------------------->\t表达式为空!");
				return;
			}

			// 为每一个表达式分配一个队列
			BlockingQueue4Files.bqf.put(pattern, new LinkedBlockingQueue<BlockingQueneFileInfo>(4096));

			logger.info("----------------------->\t [" + code + "] LinkedBlockingQueue 初始化成功! size:4096. ");

			// 相同根目录合并为一个线程
			if (!initPathTmp.equals((String) map.get("INIT_PATH")) && !initPathTmp.equals("")) {

				lp = new ArrayList<Pace>();
			}

			// 为了flume_file_log 日志记录准备
			lp.add(new Pace((String) map.get("COLLECT_PATH"), pattern, map.get("CHARACTER") + "", args[0], args[1],
					args[2], map.get("OBJ_ID") + ""));

			// 合并INIT_PATH
			rp.put((String) map.get("INIT_PATH"), lp);

			initPathTmp = (String) map.get("INIT_PATH");

			// 心跳路径
			// hb.add((String) map.get("INIT_PATH"));

		}

		System.out.println(rp);

		// 间隔一秒扫描一次
		long interval = 1000L;

		for (Entry<String, List<Pace>> entry : rp.entrySet()) {

			// 监控的根目录，合并之后的
			FileAlterationObserver observer = new FileAlterationObserver(entry.getKey(), null, null);

			// 传递必要的条件，业务处理
			observer.addListener(new MiguFileListener(entry.getValue()));

			FileAlterationMonitor monitor = new FileAlterationMonitor(interval, observer);

			// 开始监控
			try {

				logger.info(
						"\n###################################################\nmonitor started on  root directory ["
								+ entry.getKey() + "]\t\n###################################################\n");

				monitor.start();

			} catch (Exception e) {

				logger.error(e.getMessage());

				return;
			}
		}

		logger.warn("ZDY-Start-Successfully");

		new Thread(new Runnable() {
			public void run() {
				try {
					synchronized (lock) {
						logger.info("----------------------->\t文件copy队列等待初始化完成!");
						lock.wait();
						logger.info("----------------------->\t文件初始化资源释放成功!");
						FileMoveBiz.moveFile2CollectPath();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		new Thread(new Runnable() {
			public void run() {
				synchronized (lock) {
					logger.info("----------------------->\t文件初始化启动!");
					InitFile.initFileMonitor(rp, args[0], args[1], args[2]);
					lock.notifyAll();
					logger.info("----------------------->\t文件初始化完成!");
				}
			}
		}).start();
	}

	/**
	 * 
	 * 新增进程记录
	 * 
	 * @param restFulUrl
	 */
	@SuppressWarnings({ "unused", "static-access" })
	private static BaseResponse insertProcess(String[] args, String pid, int port, String restFulUrl) {
		// OBJ_ID, APP_ID, SERVER_ID, JAR_ID, PORT, KIND, PROCESS_NO, STATUS,
		// DEAL_TIME, DEAL_USER, NOTE
		String objId = UUID.randomUUID().toString().replace("-", "");

		Map<String, String> mapOj = new HashMap<String, String>();
		mapOj.put("sql", SqlConstant.INSERT_PROCESS);
		mapOj.put("param", JSONObject
				.toJSONString(new Object[] { objId, args[0], args[1], args[2], port, pid, args[3], restFulUrl }));
		String resultJson = HttpPostUtil.post(SqlConstant.EXECUTEURL, mapOj);
		// System.out.println(resultJson);
		JSONObject resObj = JSONObject.parseObject(resultJson);
		BaseResponse response = resObj.parseObject(resultJson, BaseResponse.class);
		return response;
	}

	/**
	 * 
	 * 新增进程日志表
	 * 
	 * @param restFulUrl
	 */
	@SuppressWarnings({ "unused", "static-access" })
	private static BaseResponse insertProcessLog(String[] args, String pid, int port, String restFulUrl) {
		// OBJ_ID, APP_ID, SERVER_ID, JAR_ID, PORT, KIND, PROCESS_NO, STATUS,
		// DEAL_TIME, DEAL_USER, NOTE
		String objId = UUID.randomUUID().toString().replace("-", "");

		Map<String, String> mapOj = new HashMap<String, String>();
		mapOj.put("sql", SqlConstant.INSERT_PROCESS_LOG);
		mapOj.put("param", JSONObject
				.toJSONString(new Object[] { objId, args[0], args[1], args[2], port, pid, args[3], restFulUrl }));
		String resultJson = HttpPostUtil.post(SqlConstant.EXECUTEURL, mapOj);
		// System.out.println(resultJson);
		JSONObject resObj = JSONObject.parseObject(resultJson);
		BaseResponse response = resObj.parseObject(resultJson, BaseResponse.class);
		return response;
	}
}

/**
 * SELECT t1.obj_id, t1.init_path, t1.collect_path, t1.code, t1.character,
 * t1.KIND, t1.IS_VERIFY, t1.decollator FROM flume_file t1, unify_jar_file t2
 * WHERE t2.app_id = '1' AND t2.server_id = '1' AND t2.jar_id = '1' And
 * t2.file_id = t1.obj_id;
 */

/**
 * 1 1 1 1 44444 http://192.168.129.152:8083/monitor/jdbc/queryForList.do
 * http://192.168.129.152:8083/monitor/jdbc/execute.do 20160503000000
 */
