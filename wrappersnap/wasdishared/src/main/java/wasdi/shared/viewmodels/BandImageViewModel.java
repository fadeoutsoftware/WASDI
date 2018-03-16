package wasdi.shared.viewmodels;

import java.util.List;

public class BandImageViewModel {

	private String productFileName;
	private String bandName;
	private FilterBandViewModel filterVM;
	private int filterIterationCount=1;
	private int vp_x, vp_y, vp_w, vp_h;
	private int img_w, img_h;
	private List<ProductMaskViewModel> productMasks;
	public String getProductFileName() {
		return productFileName;
	}
	public void setProductFileName(String productFileName) {
		this.productFileName = productFileName;
	}
	public String getBandName() {
		return bandName;
	}
	public void setBandName(String bandName) {
		this.bandName = bandName;
	}
	public FilterBandViewModel getFilterVM() {
		return filterVM;
	}
	public void setFilterVM(FilterBandViewModel filterVM) {
		this.filterVM = filterVM;
	}
	public int getFilterIterationCount() {
		return filterIterationCount;
	}
	public void setFilterIterationCount(int filterIterationCount) {
		this.filterIterationCount = filterIterationCount;
	}
	public int getVp_x() {
		return vp_x;
	}
	public void setVp_x(int vp_x) {
		this.vp_x = vp_x;
	}
	public int getVp_y() {
		return vp_y;
	}
	public void setVp_y(int vp_y) {
		this.vp_y = vp_y;
	}
	public int getVp_w() {
		return vp_w;
	}
	public void setVp_w(int vp_w) {
		this.vp_w = vp_w;
	}
	public int getVp_h() {
		return vp_h;
	}
	public void setVp_h(int vp_h) {
		this.vp_h = vp_h;
	}
	public int getImg_w() {
		return img_w;
	}
	public void setImg_w(int img_w) {
		this.img_w = img_w;
	}
	public int getImg_h() {
		return img_h;
	}
	public void setImg_h(int img_h) {
		this.img_h = img_h;
	}
	public List<ProductMaskViewModel> getProductMasks() {
		return productMasks;
	}
	public void setProductMasks(List<ProductMaskViewModel> productMasks) {
		this.productMasks = productMasks;
	}
}
