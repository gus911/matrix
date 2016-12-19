package cn.migu.file.core;

import java.util.regex.Pattern;

public class Pace {

	private String collectPath;
	
	private Pattern pattern;

	private String character;
	
	private String aid;
	
	private String sid;
	
	private String jid;
	
	private String oid;
	
	
	public Pace(String collectPath, Pattern pattern, String character, String aid, String sid, String jid, String oid) {
		super();
		this.collectPath = collectPath;
		this.pattern = pattern;
		this.character = character;
		this.aid = aid;
		this.sid = sid;
		this.jid = jid;
		this.oid = oid;
	}
	
	

	@Override
	public String toString() {
		return "Pace [collectPath=" + collectPath + ", pattern=" + pattern + ", character=" + character + ", aid=" + aid
				+ ", sid=" + sid + ", jid=" + jid + ", oid=" + oid + "]";
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

	public String getOid() {
		return oid;
	}

	public void setOid(String oid) {
		this.oid = oid;
	}

	public String getCharacter() {
		return character;
	}

	public void setCharacter(String character) {
		this.character = character;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}


	public String getCollectPath() {
		return collectPath;
	}

	public void setCollectPath(String collectPath) {
		this.collectPath = collectPath;
	}

}
