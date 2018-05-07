package mcjty.lib.gui.widgets;

import mcjty.lib.base.StyleConfig;
import mcjty.lib.gui.GuiParser;
import mcjty.lib.gui.RenderHelper;
import mcjty.lib.gui.Window;
import mcjty.lib.gui.events.ChoiceEvent;
import mcjty.lib.typed.Key;
import mcjty.lib.typed.Type;
import mcjty.lib.typed.TypedMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.lwjgl.input.Keyboard;

import java.util.*;
import java.util.stream.Collectors;

public class ChoiceLabel extends Label<ChoiceLabel> {

    public static final String TYPE_CHOICELABEL = "choicelabel";
    public static final Key<String> PARAM_CHOICE = new Key<>("choice", Type.STRING);
    public static final Key<Integer> PARAM_CHOICE_IDX = new Key<>("choiceIdx", Type.INTEGER);

    private List<String> choiceList = new ArrayList<>();
    private Map<String,List<String>> tooltipMap = new HashMap<>();
    private String currentChoice = null;
    private List<ChoiceEvent> choiceEvents = null;

    public ChoiceLabel(Minecraft mc, Gui gui) {
        super(mc, gui);
        setText("");
    }

    public ChoiceLabel addChoices(String ... choices) {
        for (String choice : choices) {
            choiceList.add(choice);
            if (currentChoice == null) {
                currentChoice = choice;
                setText(currentChoice);
                fireChoiceEvents(currentChoice);
            }
        }
        return this;
    }

    public ChoiceLabel setChoiceTooltip(String choice, String... tooltips) {
        tooltipMap.put(choice, Arrays.asList(tooltips));
        return this;
    }

    public ChoiceLabel setChoice(String choice) {
        currentChoice = choice;
        setText(currentChoice);
        return this;
    }

    public String getCurrentChoice() {
        return currentChoice;
    }

    @Override
    public List<String> getTooltips() {
        List<String> tooltips = tooltipMap.get(currentChoice);
        if (tooltips == null) {
            return super.getTooltips();
        } else {
            return tooltips;
        }
    }

    @Override
    public void draw(int x, int y) {
        if (!visible) {
            return;
        }
        int xx = x + bounds.x;
        int yy = y + bounds.y;

        if (isEnabled()) {
            if (isHovering()) {
                drawStyledBoxHovering(window, xx, yy, xx + bounds.width - 1, yy + bounds.height - 1);
            } else {
                drawStyledBoxNormal(window, xx, yy, xx + bounds.width - 1, yy + bounds.height - 1);
            }
            RenderHelper.drawLeftTriangle(xx + bounds.width - 10, yy + bounds.height / 2, StyleConfig.colorCycleButtonTriangleNormal);
            RenderHelper.drawRightTriangle(xx + bounds.width - 4, yy + bounds.height / 2, StyleConfig.colorCycleButtonTriangleNormal);
        } else {
            drawStyledBoxDisabled(window, xx, yy, xx + bounds.width - 1, yy + bounds.height - 1);
            RenderHelper.drawLeftTriangle(xx + bounds.width - 10, yy + bounds.height / 2, StyleConfig.colorCycleButtonTriangleDisabled);
            RenderHelper.drawRightTriangle(xx + bounds.width - 4, yy + bounds.height / 2, StyleConfig.colorCycleButtonTriangleDisabled);
        }

        super.drawOffset(x, y, -3, 1);
    }

    @Override
    public Widget mouseClick(int x, int y, int button) {
        if (isEnabledAndVisible()) {
            if (choiceList.isEmpty()) {
                return null;
            }
            int index = choiceList.indexOf(currentChoice);
            if (button == 1 || Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                index--;
                if (index < 0) {
                    index = choiceList.size()-1;
                }
            } else {
                index++;
                if (index >= choiceList.size()) {
                    index = 0;
                }
            }
            currentChoice = choiceList.get(index);
            setText(currentChoice);
            fireChoiceEvents(currentChoice);
        }
        return null;
    }

    public ChoiceLabel addChoiceEvent(ChoiceEvent event) {
        if (choiceEvents == null) {
            choiceEvents = new ArrayList<>();
        }
        choiceEvents.add(event);
        return this;
    }

    public void removeChoiceEvent(ChoiceEvent event) {
        if (choiceEvents != null) {
            choiceEvents.remove(event);
        }
    }

    private void fireChoiceEvents(String choice) {
        fireChannelEvents(TypedMap.builder()
            .put(Window.PARAM_ID, "choice")
            .put(PARAM_CHOICE, choice)
            .put(PARAM_CHOICE_IDX, choiceList.indexOf(choice))
            .build());

        if (choiceEvents != null) {
            for (ChoiceEvent event : choiceEvents) {
                event.choiceChanged(this, choice);
            }
        }
    }

    @Override
    public void readFromGuiCommand(GuiParser.GuiCommand command) {
        super.readFromGuiCommand(command);
        command.findCommand("choices").ifPresent(cmd -> {
            cmd.commands().forEach(choiceCmd -> {
                String choice = choiceCmd.getOptionalPar(0, "");
                choiceList.add(choice);
                choiceCmd.findCommand("tooltips")
                        .ifPresent(tooltipsCmd -> tooltipMap.put(choice, tooltipsCmd.parameters()
                                .map(Object::toString)
                                .collect(Collectors.toList())));
            });
        });
    }

    @Override
    public void fillGuiCommand(GuiParser.GuiCommand command) {
        super.fillGuiCommand(command);
        command.removeParameter(1); // We don't need the name as set by the label
        GuiParser.GuiCommand choicesCmd = new GuiParser.GuiCommand("choices");
        for (String s : choiceList) {
            GuiParser.GuiCommand choiceCmd = new GuiParser.GuiCommand("choice").parameter(s);
            choicesCmd.command(choiceCmd);
            List<String> tooltips = tooltipMap.get(s);
            if (tooltips != null && !tooltips.isEmpty()) {
                GuiParser.GuiCommand tooltipsCmd = new GuiParser.GuiCommand("tooltips");
                choiceCmd.command(tooltipsCmd);
                for (String tt : tooltips) {
                    tooltipsCmd.parameter(tt);
                }
            }
        }
        command.command(choicesCmd);
    }

    @Override
    public GuiParser.GuiCommand createGuiCommand() {
        return new GuiParser.GuiCommand(TYPE_CHOICELABEL);
    }

    @Override
    public <T> void setGenericValue(T value) {
        if (value instanceof Integer) {
            setChoice(choiceList.get((Integer) value));
        } else if (value instanceof Boolean) {
            setChoice(choiceList.get(((Boolean) value) ? 1 : 0));
        } else {
            super.setGenericValue(value);
        }
    }

    @Override
    public Object getGenericValue(Type type) {
        if (Type.INTEGER.equals(type)) {
            return choiceList.indexOf(getCurrentChoice());
        } else if (Type.BOOLEAN.equals(type)) {
            return choiceList.indexOf(getCurrentChoice()) != 0;
        }
        return getCurrentChoice();
    }
}
