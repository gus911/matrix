package cn.migu.file.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

public class FileMoveBiz {

	static private Logger logger = LoggerFactory.getLogger(FileMoveBiz.class);

	static private Map<Pattern, Long> p = new HashMap<Pattern, Long>();

	// 并行队列操作
	public static void moveFile2CollectPath() {
		logger.info("----------------------->\t文件copy队列启动成功!");
		for (final Entry<Pattern, BlockingQueue<BlockingQueneFileInfo>> entry : BlockingQueue4Files.bqf.entrySet()) {

			p.put(entry.getKey(), 0L);

			new Thread(new Runnable() {

				public void run() {

					BlockingQueneFileInfo bf;
					try {
						while (true) {
							// 阻塞，没有文件一直阻塞
							bf = entry.getValue().poll(1L, TimeUnit.MINUTES);
							if (bf == null) {
								p.put(entry.getKey(), p.get(entry.getKey()) + 1L);
								logger.info("----------------------->\t表达式为 [" + entry.getKey().toString() + "] 的队列,持续"
										+ p.get(entry.getKey()) + "分钟没有发现文件!");
								continue;
							};

							//如果是增量copy，直到文件大小没有发生变化,并且文件完全关闭
							BasicFileAttributes attributes = Files.readAttributes(Paths.get(bf.getPath()),
									BasicFileAttributes.class);
							
							boolean isRnOK = new File(bf.getPath()).renameTo(new File(bf.getPath()));
							Thread.sleep(200L);
							logger.info(isRnOK+"-------->"+isRnOK+"\t字节1秒内是否有变化？-------->"+(bf.getSize()!=attributes.size()));
							//注意不要把队列撑爆
							if((bf.getSize()!=attributes.size())||!isRnOK){
								//回写增量size
								bf.setSize(attributes.size());
								entry.getValue().add(bf);
								logger.info("----------------------->\t文件还在持续增加中,进入队列尾部,等待下一次copy!");
								Thread.sleep(200L);
								continue;
							}
							
							p.put(entry.getKey(), 0L);

							String[] sc = bf.getcPath().split(",");

							for (int j = 0; j < sc.length; j++) {

								String uniSuffix = UUID.randomUUID().toString();

								File tmpFile = new File(sc[j] +"/" + uniSuffix + ".tmp");

								logger.info("----------------------->\t准备写入 flume_file_log!");
								Map<String, String> mapOj = new HashMap<String, String>();
								mapOj.put("sql", SqlConstant.INSERT_FLIME_LOGS);
								mapOj.put("param",
										JSONObject.toJSONString(new Object[] { bf.getAid(), bf.getSid(), bf.getJid(),
												bf.getFid(), bf.getPath().replace("\\", "/"), bf.getfName(), bf.getSize(), bf.getNum(),
												sc[j], bf.getcTime(), bf.getEncode() }));
								String resultJson = HttpPostUtil.post(SqlConstant.EXECUTEURL, mapOj);
								JSONObject resObj = JSONObject.parseObject(resultJson);
								@SuppressWarnings("static-access")
								BaseResponse response = resObj.parseObject(resultJson, BaseResponse.class);

								// 增加一个判断，如果resultJson返回错误，联合主键冲突就不能copy文件
								if (response != null && !"00".equals(response.getResponse().getCode())) {

									logger.warn("----------------------->\t"+response.getResponse().getDesc());
									logger.warn("----------------------->\t["+bf.getfName().replace("\\", "/")+"] 已入库!联合组件冲突!");

									continue;

								}

								logger.info("----------------------->\tflume_file_log 插入成功!");

								// 重新编辑文本
								List<String> r1 = FileUtils.readLines(new File(bf.getPath()));
								
								List<String> r2 = new ArrayList<String>();
								
								int i = 0;
								
								for (String s : r1) {
									
									i++;
									
									r2.add("  "+i + "," + SqlConstant.SERVER_IP + "," + bf.getPath()
											+ "FFFFFa4b6f1e41849c064eef9d0d2a193cd00FFFFF" + s);
								}

								FileUtils.writeLines(tmpFile, "UTF-8", r2);

								// 重名写入Flume监控目录
								tmpFile.renameTo(new File(sc[j] + "/" + bf.getfName()));

								logger.info("----------------------->\t[" + bf.getPath() + "] 写入 [" + sc[j] + "/"
										+ bf.getfName() + "] 成功!" + " size: " + bf.getSize());
							}
							
							//写入JMX监控
							//写入JMX监控
							/**
							 * 1.就自定义jar而言只要知道每个正则对应的采集频率即可
							 * 2.同样就自定义jar而言只要在 flume_file表中，增加一个频率字段即可
							 * 3.对于其它类型的jar，需要另行定义频率
							 * 4.对于监控进程，通过不同的ip和端口号连接到所有运行的jar
							 * 5.监控jar本来就应该更跟任何业务无关，只要对约定好的域分析即可
							 * 6.约定好的域是一个map对象，map对象也是约定好的，只要看其变化的频率
							 */
							

						}
					} catch (InterruptedException e) {

						logger.error(e.getMessage());
					} catch (IOException e) {
						logger.error(e.getMessage());
					}
				}
			}).start();
		}
	}

//	// 心跳线程
//	public static synchronized void startHeartbeat(final List<String> hf) {
//
//		Timer timer = new Timer();
//
//		timer.scheduleAtFixedRate(new TimerTask() {
//			public void run() {
//
//				for (String h : hf) {
//
//					try {
//
//						FileUtils.deleteQuietly(new File(h + "/migu.heartbeat"));
//
//						Thread.sleep(1000L);
//
//						FileUtils.write(new File(h + "/migu.heartbeat"), System.currentTimeMillis() + "", "UTF-8",
//								false);
//
//						logger.info("----------------------->\t插入心跳文件[" + h + "/migu.heartbeat" + "]成功!");
//
//					} catch (IOException e) {
//						logger.error("------------------>\t  插入心跳文件失败!");
//					} catch (InterruptedException e) {
//						logger.error("------------------>\t  插入心跳文件失败!");
//					}
//
//				}
//
//			}
//		}, 1000, 1000 * 30L);// 延迟，间隔毫秒
//
//	}
}
