package com.iscreate.plat.datadictionary;

import java.util.List;

import com.iscreate.plat.tree.TreeNode;
import com.iscreate.plat.tree.TreeService;

public class DataDictionaryServiceImpl implements DataDictionaryService{

	private TreeService treeService;
	
	/**
	 * 根据树Id获取数据字典
	 * @param treeId
	 * @return
	 */
	public List<TreeNode> getDictionaryByTreeIdService(long treeId){
		return this.treeService.getTreeByTreeIdService(treeId);
	}
	
	/**
	 * 根据树Id获取该树的最高级数据字典
	 * @param treeId
	 * @return
	 */
	public List<TreeNode> getTheTopDictionaryByTreeIdService(long treeId){
		return this.treeService.getTheTopTreeByTreeIdService(treeId);
	}
	
	/**
	 * 根据外部内容和树的Id获取下一级数据字典
	 * @param referenceValue
	 * @param treeId
	 * @return
	 */
	public List<TreeNode> getNextDictionaryByReferenceValueAndTreeIdService(String referenceValue,long treeId){
		return this.treeService.getChildrenTreeByReferenceValueAndTreeIdService(referenceValue, treeId);
	}
	
	/**
	 * 根据外部内容和树的Id获取上一级数据字典
	 * @param referenceValue
	 * @param treeId
	 * @return
	 */
	public TreeNode getUpDictionaryByReferenceValueAndTreeIdService(String referenceValue,long treeId){
		return this.treeService.getParentTreeByReferenceValueAndTreeIdService(referenceValue, treeId);
	}
	
	/**
	 * 根据树Id获取当前树节点
	 * @param treeNodeId
	 * @return
	 */
	public TreeNode getDictionaryByTreeNodeIdService(long treeNodeId){
		return this.treeService.getTreeNodeByTreeNodeIdService(treeNodeId);
	}
	
	/**
	 * 根据树Id获取下一级树节点
	 * @param treeNodeId
	 * @return
	 */
	public List<TreeNode> getNextDictionaryByTreeNodeIdService(long treeNodeId){
		return this.treeService.getNextTreeNodeByTreeNodeIdService(treeNodeId);
	}
	
	/**
	 * 根据树Id获取上一级树节点
	 * @param treeNodeId
	 * @return
	 */
	public TreeNode getUpDictionaryByTreeNodeIdService(long treeNodeId){
		return this.treeService.getUpTreeNodeByTreeNodeIdService(treeNodeId);
	}

	public TreeService getTreeService() {
		return treeService;
	}

	public void setTreeService(TreeService treeService) {
		this.treeService = treeService;
	}
	
}
