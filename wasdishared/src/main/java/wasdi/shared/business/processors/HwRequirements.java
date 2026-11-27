package wasdi.shared.business.processors;

public class HwRequirements {
	
	private boolean gpuInference = false;
	
	private boolean gpuTraining = false;
	
	private String gpuPreferenence;
	
	private int disk;
	
	private int ram;
	


	public String getGpuPreferenence() {
		return gpuPreferenence;
	}

	public void setGpuPreferenence(String gpuPreferenence) {
		this.gpuPreferenence = gpuPreferenence;
	}

	public int getDisk() {
		return disk;
	}

	public void setDisk(int disk) {
		this.disk = disk;
	}

	public int getRam() {
		return ram;
	}

	public void setRam(int ram) {
		this.ram = ram;
	}

	public boolean isGpuInference() {
		return gpuInference;
	}

	public void setGpuInference(boolean gpuInference) {
		this.gpuInference = gpuInference;
	}

	public boolean isGpuTraining() {
		return gpuTraining;
	}

	public void setGpuTraining(boolean gpuTraining) {
		this.gpuTraining = gpuTraining;
	}
	
	

}
