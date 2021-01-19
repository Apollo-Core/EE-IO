package at.uibk.dps.ee.io.afcl;

import java.util.List;

import at.uibk.dps.afcl.Function;
import at.uibk.dps.afcl.Workflow;
import at.uibk.dps.afcl.functions.objects.DataIns;
import at.uibk.dps.afcl.functions.objects.DataOuts;
import at.uibk.dps.ee.io.validation.GraphValidation;
import at.uibk.dps.ee.model.graph.EnactmentGraph;
import at.uibk.dps.ee.model.properties.PropertyServiceData;
import at.uibk.dps.ee.model.properties.PropertyServiceData.DataType;
import net.sf.opendse.model.Communication;
import net.sf.opendse.model.Task;

/**
 * Static container for the generation of the {@link EnactmentGraph} from a
 * {@link Workflow}.
 * 
 * @author Fedor Smirnov
 *
 */
public final class GraphGenerationAfcl {

	/**
	 * Static container => no constructor
	 */
	private GraphGenerationAfcl() {
	}

	/**
	 * Generates and returns the {@link EnactmentGraph} based on the provided
	 * {@link Workflow}.
	 * 
	 * @param afclWorkflow the {@link Workflow} object created from an .afcl/.cfcl
	 *                     file
	 * @return the {@link EnactmentGraph} modeling the enactment of the workflow
	 */
	public static EnactmentGraph generateEnactmentGraph(final Workflow afclWorkflow) {
		final EnactmentGraph result = new EnactmentGraph();
		addWfInputNodes(result, AfclApiWrapper.getDataIns(afclWorkflow), AfclApiWrapper.getName(afclWorkflow));
		addWfFunctions(result, afclWorkflow);
		annotateWfOutputs(result, AfclApiWrapper.getDataOuts(afclWorkflow), afclWorkflow);
		GraphValidation.validateGraph(result);
		return result;
	}

	/**
	 * Adds the functions contained within the given workflow to the graph to the
	 * enactment graph
	 * 
	 * @param graph        the enactment graph
	 * @param afclWorkflow the given workflow
	 */
	protected static void addWfFunctions(final EnactmentGraph graph, final Workflow afclWorkflow) {
		for (final Function function : AfclApiWrapper.getWfBody(afclWorkflow)) {
			AfclCompounds.addFunctionCompound(graph, function, afclWorkflow);
		}
	}

	/**
	 * Annotates the outputs of the workflow. At this point, the nodes have to be in
	 * the graph already. Otherwise, an exception is thrown.
	 * 
	 * @param graph    the enactment graph
	 * @param dataOuts the list of afcl data outs
	 * @param workflow the afcl workflow object
	 */
	protected static void annotateWfOutputs(final EnactmentGraph graph, final List<DataOuts> dataOuts,
			final Workflow workflow) {
		for (final DataOuts dataOut : dataOuts) {
			correctDataOut(dataOut, workflow);
			annotateWfOutput(graph, dataOut);
		}
	}

	/**
	 * Corrects the src of the given data out to point to the actual data.
	 * 
	 * @param dataOut the given data out
	 * @param workflow the afcl wokflow object
	 */
	protected static void correctDataOut(final DataOuts dataOut, final Workflow workflow) {
		final String srcString = dataOut.getSource();
		final String correctSrc = HierarchyLevellingAfcl.getSrcDataId(srcString, workflow);
		dataOut.setSource(correctSrc);
	}

	/**
	 * Annotates the wf output specified by the given dataOut.
	 * 
	 * @param graph   the enactment graph
	 * @param dataOut the data out
	 */
	protected static void annotateWfOutput(final EnactmentGraph graph, final DataOuts dataOut) {
		final String source = dataOut.getSource();
		if (graph.getVertex(source) == null) {
			throw new IllegalStateException(
					"The source of the dataOut " + AfclApiWrapper.getName(dataOut) + " is not in the graph.");
		}
		final Task leafNode = graph.getVertex(source);
		PropertyServiceData.makeLeaf(leafNode);
		final String jsonKey = dataOut.getName();
		final DataType dataType = UtilsAfcl.getDataTypeForString(dataOut.getType());
		PropertyServiceData.setDataType(leafNode, dataType);
		PropertyServiceData.setJsonKey(leafNode, jsonKey);
	}

	/**
	 * Adds the data nodes defined by the provided data ins to the provided
	 * enactment graph.
	 * 
	 * @param graph   the enactment graph
	 * @param dataIns the list of data ins of the workflow
	 * @param wfName  the name of the workflow
	 */
	protected static void addWfInputNodes(final EnactmentGraph graph, final List<DataIns> dataIns,
			final String wfName) {
		for (final DataIns dataIn : dataIns) {
			graph.addVertex(generateWfInputDataNode(dataIn, wfName));
		}
	}

	/**
	 * Generates the data node modeling the provided input of the WF.
	 * 
	 * @param dataIn the data in
	 * @param wfName the name of the wf
	 * @return the data node modeling the provided input of the WF
	 */
	protected static Task generateWfInputDataNode(final DataIns dataIn, final String wfName) {
		final String dataId = dataIn.getName();
		final String nodeId = UtilsAfcl.getDataNodeId(wfName, dataId);
		final String jsonKey = AfclApiWrapper.getSource(dataIn);
		final DataType dataType = UtilsAfcl.getDataTypeForString(dataIn.getType());
		final Task result = new Communication(nodeId);
		PropertyServiceData.setDataType(result, dataType);
		PropertyServiceData.makeRoot(result);
		PropertyServiceData.setJsonKey(result, jsonKey);
		return result;
	}
}
