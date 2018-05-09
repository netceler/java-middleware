package io.insight;

import java.util.concurrent.atomic.AtomicReference;

public abstract class ServiceChain<S> {

  protected AtomicReference<LinkedService<S>> root = new AtomicReference<>();

  public void setServices(S... services) {
    root.set(link(services)[0]);
  }

  public void insertBefore(S... services) {
    LinkedService<S>[] linked = link(services);
    root.getAndUpdate(old -> {
      linked[linked.length - 1].n = old;
      return linked[0];
    });
  }

  public void appendAfter(S... services) {
    LinkedService[] array = link(services);
    root.getAndUpdate(root -> {
      if(root==null){
        return array[0];
      } else {
        LinkedService<S> last = root;
        while (last.hasNext()) {
          last = last.next();
        }
        last.n = array[0];
        return root;
      }
    });
  }

  private LinkedService<S>[] link(S[] services) {
    LinkedService[] array = new LinkedService[services.length];
    array[0] = new LinkedService<>(services[0]);
    for (int i = 1; i < array.length; i++) {
      array[i] = new LinkedService<>(services[i]);
      array[i - 1].n = array[i];
    }
    return array;
  }
}
