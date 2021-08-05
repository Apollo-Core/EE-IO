package at.uibk.dps.ee.io.afcl;

/**
 * Class to keep a reference for a given atomic function.
 * 
 * @author Fedor Smirnov
 */
public class WhileInputReference {

  protected final String firstIterationInput;
  protected final String laterIterationsInput;
  protected final String whileCompoundId;

  /**
   * Constructor method
   * 
   * @param firstIterationInput reference to data node used as src for the first
   *        iteration
   * @param laterIterationsInput reference to data node used as src for later
   *        iterations
   * @param whileCompoundId the id of the processed while compound
   */
  public WhileInputReference(final String firstIterationInput, final String laterIterationsInput,
      final String whileCompoundId) {
    super();
    this.firstIterationInput = firstIterationInput;
    this.laterIterationsInput = laterIterationsInput;
    this.whileCompoundId = whileCompoundId;
  }

  public String getFirstIterationInput() {
    return firstIterationInput;
  }

  public String getLaterIterationsInput() {
    return laterIterationsInput;
  }

  public String getWhileCompoundId() {
    return whileCompoundId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((firstIterationInput == null) ? 0 : firstIterationInput.hashCode());
    result =
        prime * result + ((laterIterationsInput == null) ? 0 : laterIterationsInput.hashCode());
    result = prime * result + ((whileCompoundId == null) ? 0 : whileCompoundId.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof WhileInputReference)) {
      return false;
    }
    final WhileInputReference other = (WhileInputReference) obj;
    return firstIterationInput.equals(other.firstIterationInput)
        && laterIterationsInput.equals(other.laterIterationsInput)
        && whileCompoundId.equals(other.whileCompoundId);
  }
}
