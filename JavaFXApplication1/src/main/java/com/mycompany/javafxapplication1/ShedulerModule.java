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
    private final int numResources; // Цей параметр можна використовувати для розподілу завдань між ресурсами

    public ShedulerModule(Collection<Task> tasks, int numResources) {
        this.tasks = new ArrayList<>(tasks);
        this.numResources = numResources;
    }

    // Симуляція FCFS (First Come First Serve) – обробляються завдання у тому порядку, в якому вони прийшли
    public long simulateFCFS() {
        long totalCompletionTime = 0;
        long currentTime = 0;
        // Завдання обробляються у порядку їх появи (як у tasks)
        for (Task task : tasks) {
            currentTime += task.getDelay();  // finish time для поточного завдання
            totalCompletionTime += currentTime;
        }
        return totalCompletionTime / 1000; // повертаємо час у секундах
    }

    // Симуляція SJF (Shortest Job First) – завдання спочатку з найменшим часом обробки
    public long simulateSJF() {
        List<Task> sjfList = new ArrayList<>(tasks);
        // Сортуємо завдання за збільшенням часу обробки (delay)
        sjfList.sort(Comparator.comparingLong(Task::getDelay));
        long totalCompletionTime = 0;
        long currentTime = 0;
        for (Task task : sjfList) {
            currentTime += task.getDelay();
            totalCompletionTime += currentTime;
        }
        return totalCompletionTime / 1000;
    }

    // Симуляція PRIORITY – загальний "ефективний час" з урахуванням пріоритету
    public long simulatePriority() {
        long totalEffectiveTime = 0;
        for (Task task : tasks) {
            // Чим менше значення priority, тим вищий пріоритет (наприклад, якщо priority = 1 – найвищий)
            totalEffectiveTime += task.getDelay() * task.getRequest().getPriority();
        }
        return totalEffectiveTime / 1000;
    }
}
