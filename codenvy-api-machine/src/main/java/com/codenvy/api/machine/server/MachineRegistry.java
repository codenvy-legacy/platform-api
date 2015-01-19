package com.codenvy.api.machine.server;

import com.codenvy.api.core.NotFoundException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Storage for created machines
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class MachineRegistry {
    private static final AtomicLong sequence = new AtomicLong(1);

    private final ConcurrentMap<String, Machine>                   machines;
    private final ConcurrentMap<String, Map<Long, CommandProcess>> processes;

    @Inject
    public MachineRegistry() {
        this.machines = new ConcurrentHashMap<>();
        this.processes = new ConcurrentHashMap<>();
    }

    /**
     * Get machine by id
     *
     * @param id
     *         machine id
     * @return machine with given id
     * @throws NotFoundException
     *         if machine with specified id is not found
     */
    public Machine getMachine(String id) throws NotFoundException {
        final Machine machine = machines.get(id);
        if (machine == null) {
            throw new NotFoundException(String.format("Machine not found %s.", id));
        }

        return machine;
    }

    public void putMachine(Machine machine) {
        machines.put(machine.getId(), machine);
    }

    public CommandProcess getProcess(String machineId, long processId) {
        return processes.get(machineId).get(processId);
    }

    public Long putProcess(String machineId, CommandProcess process) throws NotFoundException {
        final Machine machine = machines.get(machineId);
        if (machine == null) {
            throw new NotFoundException(String.format("Machine not found %s.", machineId));
        }
        processes.putIfAbsent(machineId, new HashMap<Long, CommandProcess>());

        final long id = sequence.getAndIncrement();
        process.setId(id);
        processes.get(machineId).put(id, process);

        return id;
    }
}
