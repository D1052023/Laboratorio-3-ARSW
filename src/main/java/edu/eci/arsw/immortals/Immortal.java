package edu.eci.arsw.immortals;

import edu.eci.arsw.concurrency.PauseController;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class Immortal implements Runnable {

  private final String name;
  private int health;
  private final int damage;
  private final List<Immortal> population;
  private final ScoreBoard scoreBoard;
  private final PauseController controller;

  private volatile boolean running = true;

  public Immortal(String name,
      int health,
      int damage,
      List<Immortal> population,
      ScoreBoard scoreBoard,
      PauseController controller) {

    this.name = Objects.requireNonNull(name);
    this.health = health;
    this.damage = damage;
    this.population = Objects.requireNonNull(population);
    this.scoreBoard = Objects.requireNonNull(scoreBoard);
    this.controller = Objects.requireNonNull(controller);
  }

  public String name() {
    return name;
  }

  public synchronized int getHealth() {
    return health;
  }

  public boolean isAlive() {
    return running && getHealth() > 0;
  }

  public void stop() {
    running = false;
  }

  @Override
  public void run() {
    try {
      while (running) {

        controller.awaitIfPaused();

        if (!running)
          break;

        Immortal opponent = pickOpponent();
        if (opponent == null)
          continue;

        fight(opponent);

        Thread.sleep(2);
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private Immortal pickOpponent() {

    if (population.size() <= 1)
      return null;

    Immortal other;
    int attempts = 0;

    do {
      other = population.get(
          ThreadLocalRandom.current().nextInt(population.size()));
      attempts++;
      if (attempts > population.size())
        return null;

    } while (other == this || !other.isAlive());

    return other;
  }

  private void fight(Immortal other) {

    if (other == this)
      return;

    Immortal first = this.name.compareTo(other.name) < 0 ? this : other;
    Immortal second = this.name.compareTo(other.name) < 0 ? other : this;

    synchronized (first) {
      synchronized (second) {

        if (this.health <= 0 || other.health <= 0)
          return;

        int actualDamage = Math.min(this.damage, other.health);

        // ✅ Transferencia conservativa (no se pierde energía)
        other.health -= actualDamage;
        this.health += actualDamage;

        scoreBoard.recordFight();
      }
    }
  }

}
