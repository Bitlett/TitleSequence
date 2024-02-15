package dev.bitlet.titles;

import dev.bitlet.DimaMC;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;

/**
 * <h1>TitleSequence</h1>
 * <p>An object that allows for displaying a sequence of {@link Title}s
 * asynchronously.<br><br>
 *
 * <p>The class provides methods to modify the contents of the sequence
 * that are very similar to those featured in {@link ArrayList}s.<br><br>
 *
 * <p>A sequence can be set to loop after reaching the end. There is a
 * specifiable {@link #loopPoint loopPoint}, that is the index of the title
 * in the sequence to which it will jump back to once the final title in the
 * sequence has been reached. This {@link #loopPoint loopPoint} <u>can</u>
 * be set to a value greater than the sequence size to accommodate for
 * future title additions, however if this value is found to be greater
 * than the sequence size while looping through the sequence, it will
 * simply be clamped down to the sequence size minus one.<br><br>
 *
 * <p>The time it takes for any particular title in the sequence to finish
 * displaying is based off of the {@link Title.Times} of the {@link Title}
 * at hand. That is the sum of {@link Duration}s {@code fadeIn}, {@code stay},
 * and {@code fadeOut}.<br><br>
 *
 * <p>Player logouts <strong>must</strong> be handled by calling the
 * {@link #stop(Player)} method. If this is not done while the player is being
 * shown a title sequence, it will continue trying to show the title sequence
 * to the offline player. In the case of a looping title sequence, this will
 * go on forever, permanently wasting processing power.
 *
 * @author Jay Mensink
 */
public class TitleSequence {
    /**
     * Keeps track of which players are currently being shown which title
     * sequences.
     */
    private static final Map<Player, TitleSequence> active = new HashMap<>();

    /**
     * Keeps track of which players are currently being shown this title
     * sequence, as well as the {@link BukkitTask} that is responsible for
     * showing them their next title in this sequence.
     */
    private final Map<Player, BukkitTask> displayLoops = new HashMap<>();

    /**
     * The list of {@link Title}s in this title sequence.
     */
    private final List<Title> sequence;

    /**
     * Whether this sequence is set to loop upon reaching the final title.
     */
    private boolean looping;

    /**
     * How many times the sequence should be looped before halting.
     * */
    private int loopCount;

    /**
     * The index of the title in the sequence that is considered the start of
     * the loop. When a looping title sequence reaches the end, it will find the
     * title in the sequence with this index and consider it the next title to show.
     * This <u>can</u> be set to a value greater than the sequence size to
     * accommodate for future title additions, however if this value is found to be
     * greater than the sequence size while looping through the sequence, it will
     * simply be clamped down to the sequence size minus one.
     */
    private int loopPoint;

    /**
     * Constructs an empty title sequence without any titles. It is not looping and
     * the loop point is set to index 0. The loop count is {@code Integer.MAX_VALUE}.
     */
    public TitleSequence() {
        sequence = new ArrayList<>();
        looping = false;
        loopPoint = 0;
        loopCount = Integer.MAX_VALUE;
    }

    /**
     * Constructs an empty title sequence without any titles.
     */
    public TitleSequence(boolean looping, int loopCount, int loopPoint) {
        sequence = new ArrayList<>();
        this.looping = looping;
        this.loopCount = loopCount;
        this.loopPoint = loopPoint;
    }

    /**
     * Adds a new title to the end of the sequence.
     *
     * @param title the {@link Title} to add
     * @return this title sequence
     * */
    public TitleSequence add(@NotNull Title title) {
        return add(title, sequence.size());
    }

    /**
     * Adds a new title to the sequence at a specific index.
     *
     * @param title the {@link Title} to add
     * @param index the index at which to add the title
     * @return this title sequence
     * */
    public TitleSequence add(@NotNull Title title, int index) {
        sequence.add(index, Title.title(title.title(), title.subtitle(), Title.Times.times(title.times().fadeIn(), title.times().stay().plus(Duration.ofMillis(100)), title.times().fadeOut())));
        return this;
    }

    /**
     * Sets a specific index in the sequence to a specific title. <strong>Beware that this
     * will overwrite any title existing at the specified index.</strong>
     *
     * @param title the {@link Title} to set
     * @param index the index at which to put the title
     * @return this title sequence
     * */
    public TitleSequence set(@NotNull Title title, int index) {
        sequence.set(index, title);
        return this;
    }

    /**
     * Gets the title at the specified index.
     *
     * @param index the index at which to get the title
     * @return the title at the specified index
     * */
    @Nullable
    public Title get(int index) {
        return sequence.get(index);
    }

    /**
     * Tells the sequence whether it should loop when it reaches the end.
     *
     * @param looping whether it should loop
     * @return this title sequence
     * */
    public TitleSequence setLooping(boolean looping) {
        this.looping = looping;
        return this;
    }

    /**
     * Returns whether this sequence will loop when it reaches the end.
     *
     * @return whether this title sequence is looping
     * */
    public boolean isLooping() {
        return looping;
    }

    /**
     * Set the loop point of this sequence, that is the index of the title
     * in the sequence to which it will jump back to once the final title in the
     * sequence has been reached.
     *
     * @param loopPoint the loop point of this sequence
     * @return this title sequence
     * */
    public TitleSequence setLoopPoint(int loopPoint) {
        this.loopPoint = loopPoint;
        return this;
    }

    /**
     * Get the loop point of this sequence, that is the index of the title
     * in the sequence to which it will jump back to once the final title in the
     * sequence has been reached.
     *
     * @return the loop point of this sequence
     * */
    public int getLoopPoint() {
        return loopPoint;
    }

    /**
     * Removes a title from this title sequence at the specified index.
     *
     * @param index the index of the title to remove
     * @return this title sequence
     * */
    public TitleSequence remove(int index) {
        sequence.remove(index);
        return this;
    }

    /**
     * Removes a title from this title sequence.
     *
     * @param title the title to remove
     * @return this title sequence
     * */
    public TitleSequence remove(@NotNull Title title) {
        for (Title sequenceTitle : sequence) {
            if (sequenceTitle.equals(title)) {
                sequence.remove(sequenceTitle);
                return this;
            }
        }
        return this;
    }

    /**
     * Removes all titles from this title sequence.
     *
     * @return this title sequence
     * */
    public TitleSequence clear() {
        sequence.clear();
        return this;
    }

    /**
     * Shows this title sequence to the specified player.
     *
     * @param player the player to which this title sequence will be shown
     * */
    public void show(Player player) {
        TitleSequence activeSequence = getActive(player);
        if (activeSequence != null) {
            activeSequence.stop(player, false);
        }

        if (sequence.size() == 0) {
            return;
        }

        active.put(player, this);
        displayLoops.put(player, Bukkit.getServer().getScheduler().runTask(DimaMC.getInstance(), () -> show(player, 0, 0)));
    }

    private void show(Player player, int index, int currentLoop) {
        if (index >= sequence.size()) {
            if (looping && currentLoop < loopCount) {
                ++currentLoop;
                index = Math.min(sequence.size()-1, loopPoint);
            } else {
                stop(player);
                return;
            }
        }

        Title title = sequence.get(index);
        player.showTitle(title);

        Duration titleDuration = Duration.ZERO;
        Title.Times titleTimes = title.times();
        if (titleTimes != null) {
            titleDuration = titleTimes.fadeIn().plus( titleTimes.stay() ).plus( titleTimes.fadeOut() ).minus(Duration.ofMillis(100));
        }

        long ticksToWait = (long) ((titleDuration.getSeconds() + titleDuration.getNano()/1000000000f) * 20);

        int finalIndex = index;
        int finalCurrentLoop = currentLoop;
        displayLoops.put(player, Bukkit.getServer().getScheduler().runTaskLater(DimaMC.getInstance(), () -> show(player, finalIndex +1, finalCurrentLoop), ticksToWait));
    }

    /**
     * Gets the title sequence that the specified player is currently being shown.
     *
     * @param player the player for which to get the active title sequence
     * @return the title sequence that the specified player is currently being shown
     * */
    public static TitleSequence getActive(Player player) {
        return active.get(player);
    }

    /**
     * Instantly stops the player from seeing this title sequence.
     *
     * @param player the player to which this title sequence should no longer be shown
     * */
    public void stop(Player player) {
        stop(player, true);
    }

    /**
     * Stops the player from seeing this title sequence.
     *
     * @param player the player to which this title sequence should no longer be shown
     * @param clear whether the player should have their title cleared immediately
     * */
    public void stop(Player player, boolean clear) {
        BukkitTask displayLoop = displayLoops.get(player);
        if (displayLoop == null) {
            return;
        }

        displayLoop.cancel();

        active.remove(player);
        displayLoops.remove(player);
        if (clear) {
            player.clearTitle();
        }
    }

    /**
     * Returns whether {@link Object} {@code o} is equal to this title sequence.
     *
     * @param o the object to compare to this title sequence
     * @return whether {@link Object} {@code o} is equal to this title sequence.
     * */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof TitleSequence titleSequence)) {
            return false;
        }

        if (titleSequence.sequence.size() != sequence.size()) {
            return false;
        }

        for (int i = 0; i < sequence.size(); ++i) {
            if (!( titleSequence.sequence.get(i).equals(sequence.get(i)) )) {
                return false;
            }
        }

        if (titleSequence.loopCount != loopCount) {
            return false;
        }

        if (titleSequence.looping != looping) {
            return false;
        }

        return true;
    }
}
