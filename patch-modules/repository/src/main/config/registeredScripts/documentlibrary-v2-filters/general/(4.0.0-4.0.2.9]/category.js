// Remove any trailing "/" character
if (filterData.charAt(filterData.length - 1) == "/")
{
    filterData = filterData.slice(0, -1);
}

query = '+PATH:"/cm:generalclassifiable/cm:' + search.ISO9075Encode(filterData) + '/member"';