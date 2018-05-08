package io.insight;

public class LinkedService<S> {
  public LinkedService(S s) {
    this.s = s;
  }

  S s;
  LinkedService<S> n;

  public boolean hasNext(){
    return n != null;
  }

  public LinkedService<S> next() {
    if (!hasNext()) {
      throw new IllegalStateException("no more service");
    }
    return n;
  }

  public S getService() {
    return s;
  }
}
