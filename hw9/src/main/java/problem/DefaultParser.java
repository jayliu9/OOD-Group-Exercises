package problem;

import java.util.List;

public class DefaultParser implements CommandLineParser {

  private CommandLine cmd;
  private Options options;
  private Option currentOption;
  private String currentToken;
  private List<String> expectedOpts;
  private static final int SECOND_CHARACTER = 2;

  /**
   * Parse the arguments according to the specified options.
   *
   * @param options   The specified Options.
   * @param arguments The command line arguments.
   * @return The list of option and value tokens.
   * @throws ParseException if there are any problems encountered while parsing the command line
   *                        tokens.
   */
  @Override
  public CommandLine parse(Options options, String[] arguments)
      throws ParseException {
    this.options = options;
    this.currentOption = null;
    this.expectedOpts = options.getRequiredOptions();

    this.cmd = new CommandLine();

    if (arguments != null) {
      for (String argument : arguments) {
        this.handleToken(argument);
      }
    }

    this.checkGroup();
    this.checkRequiredOptions();
    this.checkOptionValue();

    return this.cmd;
  }

  /**
   * Handle any command line token.
   *
   * @param token the command line token to handle.
   * @throws ParseException if there are any problems encountered while parsing.
   */
  private void handleToken(String token) throws ParseException {
    if (token.startsWith("--")) {
      this.currentToken = token.substring(SECOND_CHARACTER, token.length());
    }
    if (this.currentOption == null && !token.startsWith("--")) {
      throw new UnrecognizedOptionException("option should start with --: " + token);
    } else if (this.currentOption != null && this.currentOption.acceptsArg()) {
      this.handleOptionValue(token);
    } else if (token.startsWith("--")) {
      this.handleCommandOption(token);
    }
  }

  /**
   * Throw a MissingArgumentException if there's an option didn't receive argument as expected.
   *
   * @throws MissingArgumentException if there's an option didn't receive argument as expected.
   */
  private void checkOptionValue() throws MissingArgumentException {
    for (Option option : cmd.getOptions()) {
      if (option.acceptsArg() && option.getArgName() == null) {
        throw new MissingArgumentException(option);
      }
    }
  }

  /**
   * Checks whether or not the options in commandline satisfy the needs of OptionGroup.
   *
   * @throws MissingBindingOptionException if a keyOption in one group in the options is provided
   *                                       but its corresponding valueOption is not.
   * @throws MutexOptionException          if a keyOption in one group in the options is provided
   *                                       and its corresponding valueOption is also provided.
   */
  private void checkGroup()
      throws MissingBindingOptionException, MutexOptionException {
    for (Option option : this.cmd.getOptions()) {
      for (OptionGroup group : this.options.getOptionGroups()) {
        String optionName = option.getOpt();
        Option valueOption = group.getValueOption(option);
        Option keyOption = group.getKeyOption(option);

        if (group.isBinding()) {
          if (group.containsKeyOption(optionName) && !this.cmd.hasOption(valueOption.getOpt())) {
            throw new MissingBindingOptionException(option, valueOption);
          }
        } else {
          if (group.containsKeyOption(optionName) && this.cmd.hasOption(valueOption.getOpt())) {
            throw new MutexOptionException(option, valueOption);
          } else if (group.containsValueOption(optionName) && this.cmd
              .hasOption(keyOption.getOpt())) {
            throw new MutexOptionException(option, keyOption);
          }
        }
      }

      for (OptionSeries series : this.options.getOptionSeries()) {
        if (series.containsValueOption(option)) {
          Option keyOption = series.getKeyOption(option);
          if (!this.cmd.hasOption(keyOption.getOpt())) {
            throw new MissingBindingOptionException(option, keyOption);
          }
        }
      }
    }
  }

  /**
   * Throws a MissingOptionException if all of the required options are not present.
   *
   * @throws MissingOptionException if all of the required options are not present.
   */
  private void checkRequiredOptions() throws ParseException {
    if (!this.expectedOpts.isEmpty()) {
      throw new MissingOptionException(this.expectedOpts);
    }
  }

  /**
   * Updates this.expectedOpts after handling one option(Removes the option from
   * this.expectedOpts).
   *
   * @param option the handled option.
   */
  private void updateRequiredOptions(Option option) {
    if (option.isRequired()) {
      this.expectedOpts.remove(option.getOpt());
    }
  }

  /**
   * Handles an Option Value token. Sets the value of Options.
   *
   * @param token the command line token to handle
   * @throws ParseException if the token startsWith "--" or this Option cannot accept argument.
   */
  private void handleOptionValue(String token) throws ParseException {
    if (token.startsWith("--")) {
      throw new MissingArgumentException(this.currentOption);
    }
    this.currentOption.setArgName(token);
    this.currentOption = null;
  }

  /**
   * Handles an option token.
   *
   * @param token the command line token to handle
   * @throws UnrecognizedOptionException if there's no matching option in this.options.
   */
  private void handleCommandOption(String token) throws UnrecognizedOptionException {
    Option matchingOpt = this.options.getMatchingOption(token);
    if (matchingOpt == null) {
      throw new UnrecognizedOptionException("Unrecognized option: " + token + "\n");
    }
    this.handleOption(matchingOpt);
  }

  /**
   * Handles an Option.
   *
   * @param option the option to be handled.
   */
  private void handleOption(Option option) {
    // check the previous option before handling the next one
    this.updateRequiredOptions(option);
    Option newOption = new Option(option.getOpt(), option.acceptsArg(), option.isRequired(), option.getDescription());
    this.cmd.addOption(newOption);
    if (option.acceptsArg()) {
      this.currentOption = newOption;
    } else {
      this.currentOption = null;
    }
  }
}