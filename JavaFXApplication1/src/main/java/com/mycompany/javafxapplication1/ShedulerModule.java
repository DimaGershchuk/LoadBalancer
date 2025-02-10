/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

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

    public long simulateFCFS() {
        long totalTime = 0;
        for (Task task : tasks) {
            totalTime += task.getDelay();
        }
        return totalTime / 1000; 
    }

    public long simulateSJF() {
        
        List<Task> sjfList = new ArrayList<>(tasks);
        sjfList.sort(Comparator.comparingLong(Task::getDelay)); 
        long totalTime = 0;
        for (Task task : sjfList) {
            totalTime += task.getDelay();
        }
        return totalTime / 1000; 
    }

    public long simulatePriority() {
        long totalEffectiveTime = 0;
        for (Task task : tasks) {

        totalEffectiveTime += task.getDelay() * task.getRequest().getPriority();
    }
    return totalEffectiveTime / 1000; 
    }
}
