package de.uniks.beastopia.teaml.controller.menu.social;

import de.uniks.beastopia.teaml.App;
import de.uniks.beastopia.teaml.controller.AppPreparer;
import de.uniks.beastopia.teaml.rest.Group;
import de.uniks.beastopia.teaml.rest.User;
import de.uniks.beastopia.teaml.service.DataCache;
import de.uniks.beastopia.teaml.service.GroupListService;
import de.uniks.beastopia.teaml.service.TokenStorage;
import de.uniks.beastopia.teaml.utils.Prefs;
import io.reactivex.rxjava3.core.Observable;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationTest;

import javax.inject.Provider;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateGroupControllerTest extends ApplicationTest {

    @Spy
    App app;
    @Spy
    @SuppressWarnings("unused")
    final
    ResourceBundle resources = ResourceBundle.getBundle("de/uniks/beastopia/teaml/assets/lang", Locale.forLanguageTag("en"));
    @InjectMocks
    CreateGroupController createGroupController;
    @Mock
    Provider<UserController> userControllerProvider;
    @Mock
    GroupListService groupListService;
    @Mock
    TokenStorage tokenStorage;
    @SuppressWarnings("unused")
    @Mock
    Prefs prefs;
    @Mock
    DataCache cache;

    final User userOne = new User(null, null, "1", "1", null, null, null);
    final User userTwo = new User(null, null, "2", "2", null, null, null);

    @Override
    public void start(Stage stage) {
        AppPreparer.prepare(app);

        app.start(stage);
        app.show(createGroupController);
        stage.requestFocus();
    }


    @Test
    void missingGroupName() {
        assertEquals(createGroupController.groupNameField.getText().isEmpty(), createGroupController.groupNameField.getText().isEmpty());
        clickOn("#createGrpButton");
        Node dialogPane = lookup(".dialog-pane").query();
        from(dialogPane).lookup((Text t) -> t.getText().startsWith("Groupname missing")).query();
        clickOn("OK");

    }

    @Test
    void createGroupSuccessful() {
        UserController userOneController = mock();
        when(userControllerProvider.get()).thenReturn(userOneController);
        doNothing().when(userOneController).setUser(any());
        //noinspection unchecked
        ArgumentCaptor<Consumer<User>> captor = ArgumentCaptor.forClass(Consumer.class);
        when(userOneController.setOnUserToggled(captor.capture())).thenReturn(userOneController);
        when(userOneController.setOnUserPinToggled(any())).thenReturn(userOneController);
        when(userOneController.setIsAdded(anyBoolean())).thenReturn(userOneController);
        doNothing().when(userOneController).init();
        when(userOneController.render()).thenReturn(new Label(userOne.name()));
        when(cache.getAllUsers()).thenReturn(List.of(userOne, userTwo));
        when(tokenStorage.getCurrentUser()).thenReturn(userTwo);
        when(groupListService.addGroup(any(), any())).thenReturn(Observable.just(new Group(null, null, "GROUP_ID", "TestGroup", List.of(userOne._id(), userTwo._id()))));

        // search user one
        clickOn("#usernameField");
        write(userOne.name());

        // select user one
        AtomicBoolean done = new AtomicBoolean(false);
        Platform.runLater(() -> {
            captor.getValue().accept(userOne);
            done.set(true);
        });

        while (!done.get()) {
            sleep(100);
        }

        // set group name
        clickOn("#groupNameField");
        write("TestGroup");

        // create group
        clickOn("#createGrpButton");

        verify(groupListService).addGroup("TestGroup", List.of(userOne._id(), userTwo._id()));
    }

    @Test
    void back() {
        DirectMessageController mocked = mock(DirectMessageController.class);
        when(mocked.render()).thenReturn(new Pane());
        doNothing().when(mocked).init();
        app.setHistory(List.of(mocked));
        clickOn("#backButton");
        verify(mocked, times(1)).render();
    }

    @Test
    void getTitle() {
        assertEquals(app.getStage().getTitle(), resources.getString("titleNewGroup"));
    }
}
