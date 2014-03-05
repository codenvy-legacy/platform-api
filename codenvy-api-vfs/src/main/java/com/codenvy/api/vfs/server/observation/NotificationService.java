package com.codenvy.api.vfs.server.observation;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author andrew00x
 */
public class NotificationService {
    private static ErrorHandler errorHandler = new LogErrorHandler();

    private static final EventBus            eventBus  = new EventBus();
    private static final List<EventListener> listeners = new CopyOnWriteArrayList<>();

    static {
        eventBus.register(new InternalListener());
    }

    public static void setErrorHandler(ErrorHandler handler) {
        errorHandler = handler;
    }

    public static void register(EventListener listener) {
        listeners.add(listener);
    }

    public static void unregister(EventListener listener) {
        listeners.remove(listener);
    }

    public static void handleEvent(Event event) {
        eventBus.post(event);
    }

    public static class InternalListener implements EventListener {

        @AllowConcurrentEvents
        @Subscribe
        @Override
        public void create(CreateEvent event) {
            for (EventListener listener : listeners) {
                try {
                    listener.create(event);
                } catch (Throwable t) {
                    errorHandler.onError(event, t);
                }
            }
        }

        @AllowConcurrentEvents
        @Subscribe
        @Override
        public void move(MoveEvent event) {
            for (EventListener listener : listeners) {
                try {
                    listener.move(event);
                } catch (Throwable t) {
                    errorHandler.onError(event, t);
                }
            }
        }

        @AllowConcurrentEvents
        @Subscribe
        @Override
        public void rename(RenameEvent event) {
            for (EventListener listener : listeners) {
                try {
                    listener.rename(event);
                } catch (Throwable t) {
                    errorHandler.onError(event, t);
                }
            }
        }

        @AllowConcurrentEvents
        @Subscribe
        @Override
        public void delete(DeleteEvent event) {
            for (EventListener listener : listeners) {
                try {
                    listener.delete(event);
                } catch (Throwable t) {
                    errorHandler.onError(event, t);
                }
            }
        }

        @AllowConcurrentEvents
        @Subscribe
        @Override
        public void updateContent(UpdateContentEvent event) {
            for (EventListener listener : listeners) {
                try {
                    listener.updateContent(event);
                } catch (Throwable t) {
                    errorHandler.onError(event, t);
                }
            }
        }

        @AllowConcurrentEvents
        @Subscribe
        @Override
        public void updateProperties(UpdatePropertiesEvent event) {
            for (EventListener listener : listeners) {
                try {
                    listener.updateProperties(event);
                } catch (Throwable t) {
                    errorHandler.onError(event, t);
                }
            }
        }

        @AllowConcurrentEvents
        @Subscribe
        @Override
        public void updateACL(UpdateACLEvent event) {
            for (EventListener listener : listeners) {
                try {
                    listener.updateACL(event);
                } catch (Throwable t) {
                    errorHandler.onError(event, t);
                }
            }
        }
    }

    private NotificationService() {
    }
}
