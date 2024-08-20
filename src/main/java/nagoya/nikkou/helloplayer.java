package nagoya.nikkou;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Plugin(
        id = "helloplayer",
        name = "HelloPlayer",
        version = "1.0"
)
public class helloplayer {
    private final ProxyServer server;
    private final Logger logger;
    private final Set<Player> connectedPlayers = new HashSet<>();

    @Inject
    public helloplayer(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getScheduler().buildTask(this, this::checkPlayerConnectionStatus).schedule();
    }

    private void checkPlayerConnectionStatus() {
        Set<Player> currentPlayers = new HashSet<>(server.getAllPlayers());
        Set<Player> newConnectedPlayers = new HashSet<>(connectedPlayers);

        currentPlayers.stream()
                .filter(p -> !newConnectedPlayers.contains(p))
                .forEach(p -> {
                    String loginMessage = p.getUsername() + "がログインしました";
                    sendMessageToAllServers(Component.text(loginMessage).color(TextColor.fromHexString("#00FF00")));
                    newConnectedPlayers.add(p);
                });

        newConnectedPlayers.stream()
                .filter(p -> !currentPlayers.contains(p))
                .forEach(p -> {
                    String logoutMessage = p.getUsername() + "がログアウトしました";
                    sendMessageToAllServers(Component.text(logoutMessage).color(TextColor.fromHexString("#FF0000")));
                    connectedPlayers.remove(p);
                });

        connectedPlayers.clear();
        connectedPlayers.addAll(currentPlayers);

        server.getScheduler().buildTask(this, this::checkPlayerConnectionStatus)
                .delay(1, java.util.concurrent.TimeUnit.SECONDS).schedule();
    }


    private boolean hasPlayerSwitchedServer(Player player) {
        Optional<RegisteredServer> currentServer = player.getCurrentServer().map(connection -> connection.getServer());
        Optional<RegisteredServer> teamServer = player.getCurrentServer().map(connection -> connection.getServer());

        return currentServer.isPresent()
                && teamServer.isPresent()
                && !currentServer.get().equals(teamServer.get());
    }

    private void sendMessageToAllServers(Component message) {
        for (RegisteredServer registeredServer : server.getAllServers()) {
            registeredServer.sendMessage(message);
        }
    }
}