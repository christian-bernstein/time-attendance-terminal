<div align="center">
    <h1>Time-Attendance-Terminal</h1>
    <a href="https://github.com/christian-bernstein/chronos-shared">Chronos API</a>
    <span>&nbsp;&nbsp;•&nbsp;&nbsp;</span>
    <a href="https://www.prisma.io/docs/getting-started/quickstart">Download</a>
    <span>&nbsp;&nbsp;•&nbsp;&nbsp;</span>
    <a href="https://www.prisma.io/">Contact</a>
    <br />
    <hr />
</div>

Time-attendance-terminal mod for Forge 1.18. This mod is a facade for ChronosAPI.<br>
**This mod is not yet meant for public usage. It may contain critical bugs.**

<br>
<br>
<br>


## Philosophy & Motivation
Time-attendance-terminal *(TAT)* is meant to be used as a time limiter on public / semi-public minecraft servers. 
It enables every player to have the same progress compared to other players regardless of the time they can 
and wand to spend playing on a server.

TAT grants a configurable amount of playtime every day to every player. If a player is not able to play, their added time
will remain usable for a configurable amount of time, until it depletes. 

We, the authors of this mod & api used to play a lot of Minecraft over the years, but we consistently had the trouble,
that all players had different pace. Some were a lot faster than others. This meant that the projects quickly became 
boring for the faster players & those without much time felt left behind. So we decided to start incorporating a daily
time limit for our projects. But all the publicly available mods that would allow us to limit playtime weren't 
configurable & dynamic enough for us, so we started developing our own time-limiting mod.

## Features
- Configurable replenishments
  - Online time gets replenished at 0:00 every day
  - The amount of time granted is determined by the configuration
  - Every day of the week can be configured differently
- Slot system
  - The daily granted playtime can stack if not used up
  - Every day a slot is added to the history containing the amount of playtime granted for the day
  - If the slot history overflows *(More slots in history than configured)*, the oldest slots will get removed
  - Configurable amount of slots
  - The available playtime for any user is calculated by the sum of time stored in every slot
- Administrative features
  - Halt & resume your session at any time
  - Halt & resume the global timer *This can be used for server events*
  - Administrative users can join the server even if their time has depleted
  - Grand & revoke TAT-administrative rights to any player via ingame commands
  - **Note:** TAT doesn't use any preexisting / installed permission system. It solely relies on an internal permission system. *It isn't sufficient to 'just' have OP (any level), the TAT-administrative rights have to  be granted regardless'*

    
## Commands
All commands are meant to be used ingame. Console functionality might be added in any future update.
- `/chronos`
  - `time` Displays the amount of playtime you have left
  - `slots` Gives you insight in the partitioning of your time. *Useful if you want to check how much playtime might deplete the next day / day's*
  - `slots <player name>` ![](https://img.shields.io/badge/Type-admin-red)  Displays slot details about any player online
  - `admin` ![](https://img.shields.io/badge/Type-admin-red)
    - `halt` Stop your timer. If stopped, your timer won't turn back on, even after rejoining the server *(Server restart excluded)*
    - `resume` Starts your timer again. 
    - **TODO: Continue writing** 

## Updates & Issues
This mod won't be developed actively. But any issues will be resolved quickly. 
Use [GitHub Issues](https://github.com/christian-bernstein/time-attendance-terminal/issues) to let us know about any issues you encountered.
Feature requests are welcome. [Write a feature request here](https://github.com/users/christian-bernstein/projects/3). 
