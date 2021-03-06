package tr.com.srdc.cda2fhir.jolt.report.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import tr.com.srdc.cda2fhir.jolt.report.IConditionNode;
import tr.com.srdc.cda2fhir.jolt.report.ILeafNode;
import tr.com.srdc.cda2fhir.jolt.report.ILinkedNode;
import tr.com.srdc.cda2fhir.jolt.report.INode;
import tr.com.srdc.cda2fhir.jolt.report.IParentNode;
import tr.com.srdc.cda2fhir.jolt.report.IWildcardNode;
import tr.com.srdc.cda2fhir.jolt.report.JoltTemplate;
import tr.com.srdc.cda2fhir.jolt.report.Table;
import tr.com.srdc.cda2fhir.jolt.report.TableRow;
import tr.com.srdc.cda2fhir.jolt.report.Templates;

public class RootNode {
	private ParentNode root;
	private ParentNode base;

	public RootNode() {
		root = new ParentNode(null, "root");
		base = new ParentNode(root, "base");
		root.addChild(base);
	}

	public void addChild(ParentNode node) {
		base.addChild(node);
	}

	public List<INode> getChildren() {
		return null;
	}

	public ParentNode getBase() {
		return base;
	}

	public void addCondition(Condition condition) {
		root.children.get(0).addCondition(condition);
	}

	public List<Condition> getConditions() {
		return null;
	}

	public List<ILinkedNode> getLinkedNodes() {
		return root.getLinkedNodes();
	}

	public void expandLinks(Map<String, JoltTemplate> templateMap) {
		List<ILinkedNode> linkedNodes = root.getLinkedNodes();
		linkedNodes.forEach(linkedNode -> linkedNode.expandLinks(templateMap));
	}

	public void eliminateWildcardNodes() {
		List<IWildcardNode> wildcardNodes = root.getWildcardNodes();
		wildcardNodes.forEach(wildcardNode -> wildcardNode.mergeToParent());
	}

	public void conditionalize() {
		List<IConditionNode> conditionNodes = root.getConditionNodes();
		conditionNodes.forEach(conditionNode -> conditionNode.mergeToParent());
	}

	public Table toTable(Templates templates) {
		Table result = new Table();
		root.children.forEach(child -> {
			((IParentNode) child).getChildren().forEach(grandChild -> {
				List<TableRow> grandChildRows = grandChild.toTableRows(templates);
				grandChildRows.forEach(row -> {
					child.getConditions().forEach(condition -> row.addCondition(condition));
				});
				result.addRows(grandChildRows);
			});
		});
		return result;
	}

	public List<INode> getAsLinkReplacement(LinkedNode linkedNode) {
		IParentNode parent = linkedNode.getParent();
		String path = linkedNode.getPath();
		String target = linkedNode.getTarget();
		List<INode> result = new ArrayList<INode>();
		root.children.forEach(base -> {
			INode node = base.clone(parent);
			if (target.length() > 0) {
				node.promoteTargets(target);
			}
			node.setPath(path);
			result.add(node);
		});
		return result;
	}

	public void updateBase(Consumer<IParentNode> consumer) {
		List<IParentNode> children = root.children.stream().map(c -> (IParentNode) c).collect(Collectors.toList());
		children.forEach(base -> {
			consumer.accept(base);
		});
	}

	public void addRootChild(INode node) {
		root.addChild(node);
	}

	public void distributeArrays(Set<String> topPaths) {
		List<ILinkedNode> linkedNodes = root.getLinkedNodes();
		linkedNodes.forEach(linkedNode -> {
			String target = linkedNode.getTarget();
			String[] targetArrayPieces = target.split("\\[");
			if (targetArrayPieces.length < 2) {
				return;
			}
			String targetArrayName = targetArrayPieces[0];
			if (!topPaths.contains(targetArrayName)) {
				return;
			}
			String[] targetPieces = target.split("\\.");
			if (targetPieces.length != 2) {
				return;
			}
			String newTarget = targetPieces[1] + "[]";
			ILeafNode nodeAsLeaf = (ILeafNode) linkedNode;
			nodeAsLeaf.setTarget(newTarget);
		});
	}
}
