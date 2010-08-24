package br.unifor.grafico;

public class Info {
	private String starttime;
	private Long seconds;
	private Integer ctime;
	private Integer dtime;
	private Integer ttime;
	private Integer time;
	private Integer wait;
	private Integer id;

	public Info(String[] dados) {
		int i = 0;
		setStarttime(dados[i++]);
		setSeconds(new Long(dados[i++]));
		setCtime(new Integer(dados[i++]));
		setDtime(new Integer(dados[i++]));
		setTtime(new Integer(dados[i++]));
		setWait(new Integer(dados[i++]));

	}

	public String getStarttime() {
		return starttime;
	}

	public void setStarttime(String starttime) {
		this.starttime = starttime;
	}

	public Long getSeconds() {
		return seconds;
	}

	public void setSeconds(Long seconds) {
		this.seconds = seconds;
	}

	public Integer getCtime() {
		return ctime;
	}

	public void setCtime(Integer ctime) {
		this.ctime = ctime;
	}

	public Integer getDtime() {
		return dtime;
	}

	public void setDtime(Integer dtime) {
		this.dtime = dtime;
	}

	public Integer getTtime() {
		return ttime;
	}

	public void setTtime(Integer ttime) {
		this.ttime = ttime;
	}

	public Integer getTime() {
		return time;
	}

	public void setTime(Integer time) {
		this.time = time;
	}

	public Integer getWait() {
		return wait;
	}

	public void setWait(Integer wait) {
		this.wait = wait;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
