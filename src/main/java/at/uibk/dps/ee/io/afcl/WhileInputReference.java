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

  public WhileInputReference(String firstIterationInput, String laterIterationsInput,
      String whileCompoundId) {
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
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    WhileInputReference other = (WhileInputReference) obj;
    if (firstIterationInput == null) {
      if (other.firstIterationInput != null)
        return false;
    } else if (!firstIterationInput.equals(other.firstIterationInput))
      return false;
    if (laterIterationsInput == null) {
      if (other.laterIterationsInput != null)
        return false;
    } else if (!laterIterationsInput.equals(other.laterIterationsInput))
      return false;
    if (whileCompoundId == null) {
      if (other.whileCompoundId != null)
        return false;
    } else if (!whileCompoundId.equals(other.whileCompoundId))
      return false;
    return true;
  }
}
