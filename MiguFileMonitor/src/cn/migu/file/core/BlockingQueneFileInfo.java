package cn.migu.file.core;

public class BlockingQueneFileInfo {

	/**
	 * p.getAid(), p.getSid(), p.getJid(), p.getOid(), file.getPath(),
	 * file.getName() + uniSuffix, attributes.size(),
	 * CodeDetectUtil.fileLength(file.getPath()), p.getCollectPath(),
	 * FileWriteUtil.getLongDateString(attributes.creationTime().toMillis()),
	 * "UTF-8" }
	 */
	private String aid;

	private String sid;

	private String jid;

	private String fid;

	private String path;

	private String fName;

	private Long size;

	private int num;

	private String cPath;
	
	private String cTime;

	private String encode;
	
	public BlockingQueneFileInfo(String aid, String sid, String jid, String fid, String path, String fName, Long size,
			int num, String cPath, String cTime, String encode) {
		super();
		this.aid = aid;
		this.sid = sid;
		this.jid = jid;
		this.fid = fid;
		this.path = path;
		this.fName = fName;
		this.size = size;
		this.num = num;
		this.cPath = cPath;
		this.cTime = cTime;
		this.encode = encode;
	}

	@Override
	public String toString() {
		return "BlockingQueneFileInfo [aid=" + aid + ", sid=" + sid + ", jid=" + jid + ", fid=" + fid + ", path=" + path
				+ ", fName=" + fName + ", size=" + size + ", num=" + num + ", cPath=" + cPath + ", cTime=" + cTime
				+ ", encode=" + encode + "]";
	}

	public String getAid() {
		return aid;
	}

	public void setAid(String aid) {
		this.aid = aid;
	}

	public String getSid() {
		return sid;
	}

	public void setSid(String sid) {
		this.sid = sid;
	}

	public String getJid() {
		return jid;
	}

	public void setJid(String jid) {
		this.jid = jid;
	}

	public String getFid() {
		return fid;
	}

	public void setFid(String fid) {
		this.fid = fid;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getfName() {
		return fName;
	}

	public void setfName(String fName) {
		this.fName = fName;
	}

	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public String getcPath() {
		return cPath;
	}

	public void setcPath(String cPath) {
		this.cPath = cPath;
	}

	public String getcTime() {
		return cTime;
	}

	public void setcTime(String cTime) {
		this.cTime = cTime;
	}

	public String getEncode() {
		return encode;
	}

	public void setEncode(String encode) {
		this.encode = encode;
	}

	
	
}
