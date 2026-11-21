
package forge.neo.match;

import com.google.common.eventbus.Subscribe;

import forge.game.Game;
import forge.game.event.GameEventAttackersDeclared;
import forge.game.event.GameEventBlockersDeclared;
import forge.game.event.GameEventGameOutcome;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.player.RegisteredPlayer;

/**
 * Collects gameplay observations for the human player.
 *
 * Each observer belongs to one Game instance. It records observations
 * without changing game rules or AI decisions.
 */
final class ForgeMindObserver {
    private static final String SPELL_CAST = "spell_cast";
    private static final String ABILITY_ACTIVATED = "ability_activated";
    private static final String ATTACKERS_DECLARED = "attackers_declared";
    private static final String BLOCKERS_DECLARED = "blockers_declared";

    private final PlayerView human;
    private final String profileId;
    private final ForgeMindMemory memory = ForgeMindMemory.getInstance();

    private ForgeMindObserver(
            final PlayerView human,
            final String profileId) {
        this.human = human;
        this.profileId = profileId;
    }

    static void attach(
            final Game game,
            final RegisteredPlayer humanSeat) {

        if (game == null || humanSeat == null) {
            return;
        }

        Player humanPlayer = null;

        for (final Player player : game.getPlayers()) {
            if (player.getRegisteredPlayer() == humanSeat) {
                humanPlayer = player;
                break;
            }
        }

        if (humanPlayer == null) {
            System.out.println(
                    "[forgemind] Human seat not found; observations skipped");
            return;
        }

        final ForgeMindMemory memory =
                ForgeMindMemory.getInstance();
        final String profileId = memory.getOrCreateProfileId();

        game.subscribeToEvents(new ForgeMindObserver(
                PlayerView.get(humanPlayer), profileId));

        System.out.println(
                "[forgemind] Observer attached. Profile: "
                + profileId.substring(0, 8));

        logCounts(memory, profileId, "Loaded");
    }

    @Subscribe
    public void onSpellAbilityCast(
            final GameEventSpellAbilityCast event) {

        if (!isHuman(event.si().getActivatingPlayer())) {
            return;
        }

        record(event.sa().isSpell()
                ? SPELL_CAST : ABILITY_ACTIVATED);
    }

    @Subscribe
    public void onAttackersDeclared(
            final GameEventAttackersDeclared event) {

        if (isHuman(event.player())) {
            record(ATTACKERS_DECLARED);
        }
    }

    @Subscribe
    public void onBlockersDeclared(
            final GameEventBlockersDeclared event) {

        if (isHuman(event.defendingPlayer())) {
            record(BLOCKERS_DECLARED);
        }
    }

    @Subscribe
    public void onGameOutcome(
            final GameEventGameOutcome event) {

        final boolean saved = memory.save();

        System.out.println(
                "[forgemind] Game ended. Memory save: "
                + (saved ? "SUCCESS" : "FAILED"));

        logCounts(memory, profileId, "Saved");
    }

    private boolean isHuman(final PlayerView player) {
        return human.equals(player);
    }

    private void record(final String observationId) {
        memory.recordObservation(profileId, observationId);

        System.out.println(
                "[forgemind] Recorded " + observationId
                + " (count: "
                + memory.getObservationCount(profileId, observationId)
                + ")");
    }

    private static void logCounts(
            final ForgeMindMemory memory,
            final String profileId,
            final String label) {

        System.out.println(
                "[forgemind] " + label + " observations: "
                + "spells=" + memory.getObservationCount(
                        profileId, SPELL_CAST)
                + ", abilities=" + memory.getObservationCount(
                        profileId, ABILITY_ACTIVATED)
                + ", attacks=" + memory.getObservationCount(
                        profileId, ATTACKERS_DECLARED)
                + ", blocks=" + memory.getObservationCount(
                        profileId, BLOCKERS_DECLARED));
    }
}
