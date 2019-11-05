package eu.wajja.filter;

public class DetectedEntity {

	private String entity;
	private String value;

	public DetectedEntity() {

	}

	public DetectedEntity(String entity, String value) {
		this.entity = entity;
		this.value = value;
	}

	public String getEntity() {

		return entity;

	}

	public String getValue() {

		return value;

	}

	public void setValue(String value) {

		this.value = value;

	}

	public void setEntity(String entity) {

		this.entity = entity;

	}
}
