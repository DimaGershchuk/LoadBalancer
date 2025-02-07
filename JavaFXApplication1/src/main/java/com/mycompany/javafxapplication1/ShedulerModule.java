/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 *
 * @author ntu-user
 */
public class ShedulerModule {
    private final List<Task> tasks;
    private final int numResources;

    public ShedulerModule(Collection<Task> tasks, int numResources) {
        this.tasks = new ArrayList<>(tasks);
        this.numResources = numResources;
    }

    // Симуляція Round Robin
    public long simulateRoundRobin(int timeSlice) {
        List<Long> resourceTimes = new ArrayList<>(Collections.nCopies(numResources, 0L));
        Queue<Task> rrQueue = new LinkedList<>();
        // Копіюємо завдання (і скидаємо remainingTime)
        for (Task t : tasks) {
            // Створимо копію завдання з duration = delay (або fileSize, якщо є)
            rrQueue.offer(new Task(t.getRequest(), (int)t.getDelay())); // Тут використовуємо delay як тривалість
        }
        int resourceCount = 0;
        while (!rrQueue.isEmpty()) {
            Task task = rrQueue.poll();
            int assignedResource = resourceCount % numResources;
            if (task.getDelay() > timeSlice * 1000) {
                resourceTimes.set(assignedResource, resourceTimes.get(assignedResource) + timeSlice);
                // Зменшуємо "delay" на час кванту (переводимо в секунди)
                int newDelay = (int)(task.getDelay() - timeSlice * 1000);
                rrQueue.offer(new Task(task.getRequest(), newDelay));
            } else {
                resourceTimes.set(assignedResource, resourceTimes.get(assignedResource) + (int)(task.getDelay() / 1000));
            }
            resourceCount++;
        }
        return Collections.max(resourceTimes);
    }

    // Симуляція SJF (Shortest Job First)
    public long simulateSJF() {
        List<Task> sjfList = new ArrayList<>(tasks);
        sjfList.sort(Comparator.comparingLong(Task::getDelay)); // Використовуємо delay як час завдання
        long totalTime = 0;
        for (Task task : sjfList) {
            totalTime += task.getDelay();
        }
        return totalTime / 1000; // повертаємо час у секундах
    }

    // Симуляція FCFS (First Come First Serve)
    public long simulateFCFS() {
        long totalTime = 0;
        for (Task task : tasks) {
            totalTime += task.getDelay();
        }
        return totalTime / 1000;
    }
}
