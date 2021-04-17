Individual AI:
This folder contains AI scripts for single npc templates (one npc ID, but possibly many instances).  
That is, if a particular NPC/mob has a unique AI or something slightly different from all other generic NPCs, 
an individual AI script can be created for all occurences of that npc/mob.  Such individual scripts ought to be here.

Individual AI scripts can be subclassed.  In other words, an individual may inherit from a group or other individual.  
For example, one group template might define mobs that cast spells.  A particular mob may cast spells but may also 
talk whenever it gets attacked.  In that case, instead of writing all the AI for attacking and casting spells, it may 
inherit from a group template that defines AI for ALL mobs that cast spells, then add behaviours for talking onAttack.

"NPC registrations" refers to the addition of NPCs in the various events of the scripts, such as onAttack, onKill, etc
Those are done by using keywords such as "addKillId" etc.  For more details on registrations, see 
"scripts/quests/documentation.txt"
