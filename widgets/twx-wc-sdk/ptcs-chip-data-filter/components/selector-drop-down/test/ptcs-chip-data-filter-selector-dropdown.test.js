/* eslint-disable no-unused-expressions */
import {fixture, expect} from '@open-wc/testing/index.js';
import {PTCS} from 'ptcs-library/library.js';
import moment from 'ptcs-moment/moment-import.js';
import 'ptcs-base-theme/ptcs-base-theme.js';
import '../ptcs-chip-data-filter-selector-dropdown.js';

const data = {
    dataShape: {
        fieldDefinitions: {
            TextBasedCategory: {
                name:     'textBasedCategory',
                baseType: 'STRING'
            },
            DateTimeBasedCategory: {
                name:     'dateTimeBasedCategory',
                baseType: 'DATETIME'
            },
            NumberBasedCategory: {
                name:     'numberBasedCategory',
                baseType: 'NUMBER'
            },
            IntegerBasedCategory: {
                name:     'integerBasedCategory',
                baseType: 'INTEGER'
            },
            BooleanBasedCategory: {
                name:     'booleanBasedCategory',
                baseType: 'BOOLEAN'
            },
            LocationBasedCategory: {
                name:     'locationBasedCategory',
                baseType: 'LOCATION'
            },
            DefaultBasedCategory: {
                name:     'defaultBasedCategory',
                baseType: 'XXX'
            },
            UnsupportedBasedCategory: {
                name:     'unsupportedBasedCategory',
                baseType: 'infotable'
            }
        }
    }
};

const metaData = {
    dataShape: {
        fieldDefinitions: {
            sourceType: {
                name:     'sourceType',
                aspects:  {},
                baseType: 'STRING',
            },
            location: {
                name:     'location',
                aspects:  {},
                baseType: 'LOCATION'
            },
            source: {
                name:     'source',
                aspects:  {},
                baseType: 'STRING'
            },
            key: {
                name:    'key',
                aspects: {
                    isPrimaryKey: true
                },
                baseType: 'STRING'
            },
            tags: {
                name:     'tags',
                baseType: 'TAGS'
            },
            timestamp: {
                name:     'timestamp',
                aspects:  {},
                baseType: 'DATETIME'
            },
            field1: {
                name:     'field1',
                baseType: 'STRING',
                ordinal:  1,
                aspects:  {
                    isPrimaryKey: true
                }
            }
        }
    }
};

const columnFormat = JSON.stringify({
    TextBasedCategory: {
        name:     'textBasedCategory',
        baseType: 'STRING'
    },
    DateTimeBasedCategory: {
        name:     'dateTimeBasedCategory',
        baseType: 'DATETIME'
    },
    NumberBasedCategory: {
        name:     'numberBasedCategory',
        baseType: 'NUMBER'
    },
    IntegerBasedCategory: {
        name:            'integerBasedCategory',
        Title:           '[[integer]]',
        baseType:        'INTEGER',
        __showThisField: false
    },
    BooleanBasedCategory: {
        name:     'booleanBasedCategory',
        baseType: 'BOOLEAN'
    },
    LocationBasedCategory: {
        name:     'locationBasedCategory',
        baseType: 'LOCATION'
    },
    DefaultBasedCategory: {
        name:     'defaultBasedCategory',
        baseType: 'YYY'
    },
    UnsupportedBasedCategory: {
        name:     'unsupportedBasedCategory',
        baseType: 'infotable'
    }
});

const dataWithLocalization = {
    dataShape: {
        fieldDefinitions: {
            TextBasedCategory: {
                name:     'textBasedCategory',
                baseType: 'STRING'
            },
            DateTimeBasedCategory: {
                name:     'dateTimeBasedCategory',
                Title:    'tw.date',
                baseType: 'DATETIME'
            },
            NumberBasedCategory: {
                name:     'numberBasedCategory',
                baseType: 'NUMBER'
            },
            IntegerBasedCategory: {
                name:     'integerBasedCategory',
                Title:    '[[integer]]',
                baseType: 'INTEGER'
            },
            BooleanBasedCategory: {
                name:     'booleanBasedCategory',
                baseType: 'BOOLEAN'
            },
            LocationBasedCategory: {
                name:     'locationBasedCategory',
                baseType: 'LOCATION'
            },
            DefaultBasedCategory: {
                name:     'defaultBasedCategory',
                baseType: 'YYY'
            },
            UnsupportedBasedCategory: {
                name:     'unsupportedBasedCategory',
                baseType: 'infotable'
            }
        }
    }
};

function getDateQueryChip(type, fieldName, dataEnteredByUser) {
    const retObj = {fieldName: fieldName};
    const parseToTimestamp = (day) => Date.parse(day);

    switch (type) {
        case 'string':
        case 'number':
        case 'boolean':
            console.log ('Type ' + type + ' not supported yet');
            break;
        case 'datetime': {
            switch (dataEnteredByUser.operation) {
                case 'between':
                    retObj.type = 'BETWEEN';
                    retObj.from = parseToTimestamp(dataEnteredByUser.from);
                    retObj.to = parseToTimestamp(dataEnteredByUser.to);
                    break;
                case 'equals':
                    retObj.type = 'EQ';
                    retObj.value = parseToTimestamp(dataEnteredByUser.date);
                    break;
                case 'before':
                    retObj.type = 'LT';
                    retObj.value = parseToTimestamp(dataEnteredByUser.date);
                    break;
                case 'beforeEq':
                    retObj.type = 'LE';
                    retObj.value = parseToTimestamp(dataEnteredByUser.date);
                    break;
                case 'after':
                    retObj.type = 'GT';
                    retObj.value = parseToTimestamp(dataEnteredByUser.date);
                    break;
                case 'afterEq':
                    retObj.type = 'GE';
                    retObj.value = parseToTimestamp(dataEnteredByUser.date);
                    break;
                case 'notEq':
                    retObj.type = 'NE';
                    retObj.value = parseToTimestamp(dataEnteredByUser.date);
                    break;
                case 'within': {
                    const toDate = moment(); // current time
                    const fromDate = moment(toDate).add(-1 * dataEnteredByUser.value, dataEnteredByUser.units);

                    retObj.type = 'BETWEEN';
                    retObj.from = parseToTimestamp(fromDate);
                    retObj.to = parseToTimestamp(toDate);
                    break;
                }
            }
            break;
        }
        default:
            console.error('Unknown type: ' + type, dataEnteredByUser);
    }

    return retObj;
}

function findIndexForGivenType(refType, d) {
    let indexOfInterest;

    if (!d) {
        d = data;
    }

    Object.values(d.dataShape.fieldDefinitions).some((el, index) => {
        if (el.baseType.toLowerCase() === refType) {
            indexOfInterest = index;
            return true;
        }
        return false;
    });

    return indexOfInterest;
}

function findDataCategoryForGivenType(refType, d) {
    let dataCategory;

    if (!d) {
        d = data;
    }

    Object.values(d.dataShape.fieldDefinitions).some((el, index) => {
        if (el.baseType.toLowerCase() === refType) {
            dataCategory = el;
            return true;
        }
        return false;
    });

    return dataCategory;
}

function dataShapeDef(category, def) {
    const ds = {dataShape: {fieldDefinitions: {}}};
    ds.dataShape.fieldDefinitions[category] = def;
    return ds;
}

describe('<ptcs-chip-data-filter-selector-dropdown>-<text-case>', () => {
    const stringIndex = findIndexForGivenType('string');
    let textCompInStringCaseComp, textOperationsCompInStringCaseComp, stringCaseComp, selector, container, mainDropDown, applyButton;

    beforeEach(async() => {
        selector = await fixture(`
            <ptcs-chip-data-filter-selector-dropdown dictionary='{"stringBetween": "Between", "stringOutside": "Outside"}' tabindex="0">
            </ptcs-chip-data-filter-selector-dropdown>`
        );
        selector.sortFilters = false;
        selector.customBaseTypesMapping = {default: 'string', infotable: 'unsupported', integer: 'number'};
        selector.data = data;

        await PTCS.wait();

        mainDropDown = selector.$['main-drop-down'];
        mainDropDown.selectedIndexes = [stringIndex];

        await PTCS.wait();

        applyButton = selector.$['apply-button'];
        container = selector.shadowRoot.getElementById('container');
        stringCaseComp = container.querySelector('#string-case');
        textOperationsCompInStringCaseComp = stringCaseComp.shadowRoot.getElementById('drop-down');
        textCompInStringCaseComp = stringCaseComp.$['text-field'];
    });

    it('is setting non-existing operator', async() => {
        let bInsideHandler = false;
        const handler = event => {
            bInsideHandler = true;
            selector.removeEventListener('change', handler);
        };

        await PTCS.wait();

        textCompInStringCaseComp.text = 'not important what to put here';
        textOperationsCompInStringCaseComp.selectedValue = 'nonExistingOperation';
        selector.addEventListener('change', handler);
        await PTCS.wait();
        selector.$['apply-button'].click();

        // we don't expect handler will be fired since no filter change should occur
        expect(bInsideHandler).to.be.false;
        selector.removeEventListener('change', handler);
        mainDropDown.selectedIndexes = [stringIndex];

        await PTCS.wait(200);

        expect(textCompInStringCaseComp.text).to.be.eql('');
        stringCaseComp.dataEnteredByUser = null;
        expect(textCompInStringCaseComp.text).to.be.eql('');
    });

    it('does Apply button behave correctly 1', async() => {
        await applyButton.updateComplete;
        expect(applyButton.hasAttribute('disabled')).to.be.eql(true);

        const textToBeInserted = 'an example text';
        textCompInStringCaseComp.text = textToBeInserted;
        await textCompInStringCaseComp.updateComplete;
        await applyButton.updateComplete;

        expect(applyButton.hasAttribute('disabled')).to.be.eql(false);

        textCompInStringCaseComp.text = '';
        await textCompInStringCaseComp.updateComplete;
        await applyButton.updateComplete;

        expect(applyButton.hasAttribute('disabled')).to.be.eql(true);
    });
});

describe('<ptcs-chip-data-filter-selector-dropdown>-<text-select-case>', () => {
    let textSelectCompInStringCaseComp, stringCaseComp, selector, container, mainDropDown, applyButton;

    const selectOptions = 'val1:apples,val2:oranges,cotton candy grapes:cotton candy grapes';
    const textDropdownData = dataShapeDef('cat', {
        name:     'textSelect',
        baseType: 'STRING',
        aspects:  {selectOptions}
    });
    const textDropdownDataWithDefault = dataShapeDef('cat', {
        name:     'textSelect',
        baseType: 'STRING',
        aspects:  {selectOptions, defaultValue: 'val2'}
    });

    beforeEach(async() => {
        selector = await fixture(`
            <ptcs-chip-data-filter-selector-dropdown dictionary='{"stringBetween": "Between", "stringOutside": "Outside"}' tabindex="0">
            </ptcs-chip-data-filter-selector-dropdown>`
        );

        selector._renderSubcomponents = true; // Trigger generation of *-case subcomponents
        await selector.updateComplete;
    });

    const assertSelectVisible = () => {
        const textContainer = stringCaseComp.shadowRoot.getElementById('string-text-container');
        const selectContainer = stringCaseComp.shadowRoot.getElementById('string-select-container');
        expect(textContainer.hasAttribute('hidden')).to.be.eql(true);
        expect(selectContainer.hasAttribute('hidden')).to.be.eql(false);
    };

    const assertItems = () => {
        const items = textSelectCompInStringCaseComp.items;
        expect(items.length).to.be.eql(3, 'textSelectCompInStringCaseComp has 3 items');
        expect(items[0]).to.be.eql({val: 'val1', label: 'apples'});
        expect(items[1]).to.be.eql({val: 'val2', label: 'oranges'});
        expect(items[2]).to.be.eql({val: 'cotton candy grapes', label: 'cotton candy grapes'});
    };

    it('parses and passes selectOptions displaying dropdown control', async() => {
        selector.data = textDropdownData;
        await selector.updateComplete;

        mainDropDown = selector.shadowRoot.getElementById('main-drop-down');
        mainDropDown.selectedIndexes = [0];

        // The event listener for selectedIndexesChanged uses several levels of promises.
        await PTCS.wait(200);

        applyButton = selector.shadowRoot.getElementById('apply-button');
        container = selector.shadowRoot.getElementById('container');
        stringCaseComp = container.querySelector('#string-case');
        textSelectCompInStringCaseComp = stringCaseComp.shadowRoot.getElementById('string-select-field');
        expect(textSelectCompInStringCaseComp.selectedIndexes).to.be.eql([]);
        assertSelectVisible();
        assertItems();
    });

    it('parses, passes selectOptions and applies default in dropdown', async() => {
        selector.data = textDropdownDataWithDefault;
        await selector.updateComplete;
        await PTCS.wait();

        mainDropDown = selector.shadowRoot.getElementById('main-drop-down');
        mainDropDown.selectedIndexes = [0];
        await PTCS.wait(200);

        applyButton = selector.shadowRoot.getElementById('apply-button');
        container = selector.shadowRoot.getElementById('container');
        stringCaseComp = container.querySelector('#string-case');
        textSelectCompInStringCaseComp = stringCaseComp.shadowRoot.getElementById('string-select-field');

        expect(applyButton.hasAttribute('disabled')).to.be.eql(false, 'Apply button is enabled');
        expect(textSelectCompInStringCaseComp.selectedIndexes).to.be.eql([1]);
        expect(stringCaseComp.getFormatted()).to.be.eql('is exactly: oranges');
        assertItems();
    });

    it('does Apply button behave correctly 2', async() => {
        selector.data = textDropdownData;
        await selector.updateComplete;

        container = selector.shadowRoot.getElementById('container');
        stringCaseComp = container.querySelector('#string-case');
        textSelectCompInStringCaseComp = stringCaseComp.shadowRoot.getElementById('string-select-field');
        mainDropDown = selector.shadowRoot.getElementById('main-drop-down');
        mainDropDown.selectedIndexes = [0];

        await PTCS.wait(200);

        applyButton = selector.shadowRoot.getElementById('apply-button');
        expect(applyButton.hasAttribute('disabled')).to.be.eql(true, 'Apply button is not enabled');

        textSelectCompInStringCaseComp.selected = [0];
        await PTCS.wait(100);

        // expect(applyButton.disabled).to.be.eql(false, 'Apply button is not disabled');
        expect(applyButton.hasAttribute('disabled')).to.be.eql(false);
    });

    it('handles dataEnteredByUser correctly', async() => {
        selector.data = textDropdownData;
        await selector.updateComplete;
        await PTCS.wait(100);

        container = selector.shadowRoot.getElementById('container');
        stringCaseComp = container.querySelector('#string-case');
        stringCaseComp.dataEnteredByUser = {operation: 'And', value: 'val1'};

        await PTCS.wait(200);

        expect(stringCaseComp.__currentSelectionStringDropDown.val).to.be.eql('val1');
    });
});

describe('<ptcs-chip-data-filter-selector-dropdown>-<number-case>', () => {
    let selector, mainDropDown, applyButton, container;
    let numberCaseComp, numberDropDown, fromTextField, toTextField;
    let expectedDataContent, expectedDataIsError, expectedQuery;
    const handler = event => {
        const passedData = event.detail.data;
        const dataOfInterest = passedData && passedData.length && passedData[passedData.length - 1];
        const dataEnteredByUser = dataOfInterest && dataOfInterest.dataEnteredByUser;
        const isError = dataOfInterest && dataOfInterest.isError;
        const query = JSON.stringify(selector.query);

        expect(dataEnteredByUser).to.be.eql(expectedDataContent);
        expect(isError).to.be.eql(expectedDataIsError);
        expect(query).to.be.eql(expectedQuery);
        selector.removeEventListener('change', handler);
    };
    const numberIndex = findIndexForGivenType('integer');
    const dataCategory = findDataCategoryForGivenType('integer');

    function findIndexOfItemInDropDown(item) {
        let indexOfInterest;

        numberDropDown.items.some((el, index) => {
            if (el.name === item) {
                indexOfInterest = index;
                return true;
            }
            return false;
        });

        return indexOfInterest;
    }

    beforeEach(async() => {
        selector = await fixture(`
            <ptcs-chip-data-filter-selector-dropdown dictionary='{"stringBetween": "Between", "stringOutside": "Outside"}' tabindex="0">
            </ptcs-chip-data-filter-selector-dropdown>`
        );
        selector.sortFilters = false;
        selector.customBaseTypesMapping = {default: 'string', infotable: 'unsupported', integer: 'number'};
        selector.data = data;
        selector._renderSubcomponents = true; // Trigger generation of *-case subcomponents
        mainDropDown = selector.$['main-drop-down'];
        mainDropDown.selectedIndexes = [numberIndex]; // select the number type based filter option
        applyButton = selector.$['apply-button'];
        container = selector.shadowRoot.getElementById('container');
        numberCaseComp = container.querySelector('#number-case');

        // fromTextField = numberCaseComp.$['from-text-field'];
        // toTextField = numberCaseComp.$['to-text-field'];
        // numberDropDown = numberCaseComp.shadowRoot.getElementById('drop-down');
    });

    it('is submenu populated correctly', async() => {
        await PTCS.wait();

        const desiredItemArray = ['=', '≠', '>', '<', '>=', '<=', 'Between', 'Outside'];

        container = selector.shadowRoot.getElementById('container');
        numberCaseComp = container.querySelector('#number-case');
        numberDropDown = numberCaseComp.shadowRoot.getElementById('drop-down');

        expect(numberDropDown.items.length).to.be.eql(desiredItemArray.length);
        for (const option of numberDropDown.items) {
            expect(desiredItemArray.indexOf(option.label) >= 0).to.be.true;
        }
    });

    it('is saving handled properly & query generated correctly, single value', async() => {
        await PTCS.wait();

        // unhappy path
        let valToBeInserted = '';
        const dropDownItemSelection = '>';
        const dropDownItemSelectionIndex = findIndexOfItemInDropDown(dropDownItemSelection);
        expectedDataContent = {
            operation: dropDownItemSelection,
            value:     valToBeInserted
        };
        expectedDataIsError = true;
        expectedQuery = 'null';

        selector.addEventListener('change', handler);
        numberDropDown.selectedIndexes = []; // Force change event
        numberDropDown.selectedIndexes = [dropDownItemSelectionIndex];

        container = selector.shadowRoot.getElementById('container');
        numberCaseComp = container.querySelector('#number-case');

        fromTextField = numberCaseComp.$['from-text-field'];
        fromTextField.text = valToBeInserted;
        selector.$['apply-button'].click();
        mainDropDown.selectedIndexes = []; // Force change event
        mainDropDown.selectedIndexes = [numberIndex];

        await PTCS.wait();
        expect(fromTextField.text).to.be.eql('');

        // happy path
        valToBeInserted = 90;
        expectedDataContent = {
            operation: dropDownItemSelection,
            value:     valToBeInserted
        };
        expectedDataIsError = false;
        expectedQuery = JSON.stringify({
            filters: {
                type:    'And',
                filters: [{
                    fieldName: dataCategory.name,
                    type:      'GT',
                    value:     valToBeInserted
                }]
            }
        });
        numberDropDown.selectedIndexes = []; // Force change event
        numberDropDown.selectedIndexes = [dropDownItemSelectionIndex];
        fromTextField.text = valToBeInserted;
        selector.addEventListener('change', handler);
        selector.$['apply-button'].click();
        mainDropDown.selectedIndexes = []; // Force change event
        mainDropDown.selectedIndexes = [numberIndex];
        await PTCS.wait();
        expect(fromTextField.text).to.be.eql('');
    });

    it('is saving handled properly & query generated correctly, notBetween case', async() => {
        await PTCS.wait();

        const firstValToBeInserted = 90;
        const secondValToBeInserted = 10;
        const dropDownItemSelection = 'notBetween';
        const dropDownItemSelectionIndex = findIndexOfItemInDropDown(dropDownItemSelection);
        expectedDataContent = {
            operation: dropDownItemSelection,
            from:      firstValToBeInserted,
            to:        secondValToBeInserted
        };
        expectedDataIsError = false;
        expectedQuery = JSON.stringify({
            filters: {
                type:    'And',
                filters: [{
                    fieldName: dataCategory.name,
                    type:      'NOTBETWEEN',
                    from:      firstValToBeInserted,
                    to:        secondValToBeInserted
                }]
            }
        });
        // positive case, correct data
        selector.addEventListener('change', handler);
        numberDropDown.selectedIndexes = []; // Force change event
        await PTCS.wait(200);
        numberDropDown.selectedIndexes = [dropDownItemSelectionIndex];
        await PTCS.wait(200);

        numberCaseComp = selector.shadowRoot.getElementById('container').querySelector('#number-case');
        numberCaseComp.dataEnteredByUser = expectedDataContent;

        await PTCS.wait();

        fromTextField = numberCaseComp.$['from-text-field'];
        toTextField = numberCaseComp.$['to-text-field'];
        expect(Number(fromTextField.text)).to.be.eql(firstValToBeInserted, 'should be dataEnteredByUser firstValToBeInserted');
        expect(Number(toTextField.text)).to.be.eql(secondValToBeInserted, 'should be dataEnteredByUser secondValToBeInserted ');

        mainDropDown.selectedIndexes = []; // Force change event
        await PTCS.wait(200);
        mainDropDown.selectedIndexes = [numberIndex];
        await PTCS.wait(200);

        expect(fromTextField.text).to.be.eql('');
        expect(toTextField.text).to.be.eql('');
    });

    it('does Apply button behave correctly 3', async() => {

        await PTCS.wait();
        await applyButton.updateComplete;

        expect(applyButton.hasAttribute('disabled')).to.be.eql(true);

        const valToBeInserted = 92;

        numberCaseComp = selector.shadowRoot.getElementById('container').querySelector('#number-case');
        fromTextField = numberCaseComp.$['from-text-field'];
        toTextField = numberCaseComp.$['to-text-field'];
        numberDropDown = numberCaseComp.shadowRoot.getElementById('drop-down');

        fromTextField.text = '' + valToBeInserted;

        await fromTextField.updateComplete;
        await applyButton.updateComplete;
        await PTCS.wait();

        // This was the previous test. I suspect it was incorrect
        // expect(applyButton.hasAttribute('disabled')).to.be.eql(true); --- why should it be true?!?
        expect(applyButton.hasAttribute('disabled')).to.be.eql(false);
        fromTextField.text = '';

        await fromTextField.updateComplete;
        await applyButton.updateComplete;
        await PTCS.wait();

        expect(applyButton.hasAttribute('disabled')).to.be.eql(true);
        fromTextField.text = '' + valToBeInserted;

        numberDropDown.selectedValue = 'between';
        await fromTextField.updateComplete;
        await applyButton.updateComplete;

        expect(applyButton.hasAttribute('disabled')).to.be.eql(true);

        toTextField.text = '' + valToBeInserted;
        await toTextField.updateComplete;
        await applyButton.updateComplete;

        expect(applyButton.hasAttribute('disabled')).to.be.eql(false);
        fromTextField.text = '';
        await fromTextField.updateComplete;
        await applyButton.updateComplete;

        expect(applyButton.hasAttribute('disabled')).to.be.eql(true);
        toTextField.text = '';
        await toTextField.updateComplete;
        await applyButton.updateComplete;

        expect(applyButton.hasAttribute('disabled')).to.be.eql(true);
        fromTextField.text = '' + valToBeInserted;
        await fromTextField.updateComplete;
        await applyButton.updateComplete;

        expect(applyButton.hasAttribute('disabled')).to.be.eql(true);

        // back to the default selection
        numberDropDown.selectedIndexes = []; // Force change event
        numberDropDown.selectedIndexes = [0];
        await applyButton.updateComplete;

        await PTCS.wait();

        expect(applyButton.hasAttribute('disabled')).to.be.eql(false);
    });

    it('test bad number conditions', async() => {
        await PTCS.wait();

        numberCaseComp = selector.shadowRoot.getElementById('container').querySelector('#number-case');
        numberDropDown = numberCaseComp.shadowRoot.getElementById('drop-down');

        numberCaseComp.dataEnteredByUser = null;

        await PTCS.wait();

        expect(applyButton.hasAttribute('disabled')).to.be.eql(true);
        numberCaseComp.dataEnteredByUser = {operation: ''};

        await PTCS.wait();

        expect(applyButton.hasAttribute('disabled')).to.be.eql(true);
        numberCaseComp.dataEnteredByUser = {operation: 'between'};

        await PTCS.wait();

        expect(applyButton.hasAttribute('disabled')).to.be.eql(true);
        numberCaseComp.dataEnteredByUser = {operation: '='};
        await PTCS.wait();
        expect(applyButton.hasAttribute('disabled')).to.be.eql(true);
    });
});

describe('<ptcs-chip-data-filter-selector-dropdown>-<boolean-case>', () => {
    const booleanIndex = findIndexForGivenType('boolean');
    const dataCategory = findDataCategoryForGivenType('boolean');

    let selector, mainDropDown, booleanCaseComp, booleanDropdown, container;

    beforeEach(async() => {
        selector = await fixture(`
            <ptcs-chip-data-filter-selector-dropdown dictionary='{"stringTrue": "True", "stringFalse": "False"}' condition-label="Y/N" tabindex="0">
            </ptcs-chip-data-filter-selector-dropdown>`
        );
        selector.sortFilters = false;
        selector.customBaseTypesMapping = {default: 'string', infotable: 'unsupported', integer: 'number'};
        selector.data = data;

        await selector.updateComplete;
        await PTCS.wait();

        mainDropDown = selector.$['main-drop-down'];
        mainDropDown.selectedIndexes = [booleanIndex]; // select the boolean  type based filter option

        await PTCS.wait(200);

        container = selector.shadowRoot.getElementById('container');
        booleanCaseComp = container.querySelector('#boolean-case');
        booleanDropdown = booleanCaseComp.shadowRoot.getElementById('drop-down');
    });

    it('is saving handled correctly & query is generated correctly', async function() {
        await PTCS.wait();
        let booleanSelection, expectedQuery, expectedData;
        const handler = event => {
            const passedData = event.detail.data;
            const dataOfInterest = passedData && passedData.length && passedData[passedData.length - 1];
            const dataEnteredByUser = dataOfInterest && dataOfInterest.dataEnteredByUser;
            const isError = dataOfInterest && dataOfInterest.isError;
            const selectorQuery = JSON.stringify(selector.query);

            expect(selectorQuery).to.be.eql(expectedQuery);
            expect(dataEnteredByUser).to.be.eql(expectedData);
            expect(isError).to.be.eql(false);
            selector.removeEventListener('change', handler);
        };

        mainDropDown.selectedIndexes = [booleanIndex];
        await PTCS.wait(200);

        // true & false choice
        selector.addEventListener('change', handler);

        booleanSelection = 'true';
        booleanCaseComp.dataEnteredByUser = booleanSelection;
        await PTCS.wait();

        expectedData = booleanSelection;
        expectedQuery = JSON.stringify({
            filters: {
                type:    'And',
                filters: [{
                    fieldName: dataCategory.name,
                    type:      'EQ',
                    value:     booleanSelection
                }]
            }
        });

        selector.$['apply-button'].click();
        await PTCS.wait();
        mainDropDown.selectedIndexes = [booleanIndex];

        await PTCS.wait(200);
        expect(booleanDropdown).to.not.be.eql(null, 'booleanDropdown resolves');
        expect(booleanDropdown.selectedValue).to.be.eql('true', 'selectedValue is true');

        // only false choice
        selector.addEventListener('change', handler);
        booleanSelection = 'false';
        booleanCaseComp.dataEnteredByUser = booleanSelection;
        await PTCS.wait();

        expectedData = booleanSelection;
        expectedQuery = JSON.stringify({
            filters: {
                type:    'And',
                filters: [{
                    fieldName: dataCategory.name,
                    type:      'EQ',
                    value:     'true'
                }, {
                    fieldName: dataCategory.name,
                    type:      'EQ',
                    value:     booleanSelection
                }]
            }
        });

        await PTCS.wait();

        selector.shadowRoot.getElementById('apply-button').click();
        mainDropDown.selectedIndexes = [booleanIndex];

        await PTCS.wait(200);

        expect(booleanCaseComp.queryFieldName()).to.be.eql('booleanBasedCategory');
        expect(booleanCaseComp.getFormatted()).to.be.eql('True');
    });

    it('focusedElements', async function() {
        selector.setAttribute('tabindex', '0');
        selector.mode = 'open';

        await PTCS.wait();

        const focusable = selector.focusableElements.map(el => el.getAttribute('id'));
        expect(focusable).to.be.eql(['main-drop-down', 'drop-down', 'apply-button', 'cancel-button']);
    });
});

describe('<ptcs-chip-data-filter-selector-dropdown>-<datetime-case>', () => {
    const dateTimeIndex = findIndexForGivenType('datetime');
    const dataCategory = findDataCategoryForGivenType('datetime');
    let selector, mainDropDown, applyButton, container;
    let operationsDropDown, datePicker;
    let dateToBeInserted, withinValue, withinUnit;

    beforeEach(async() => {
        selector = await fixture(`
            <ptcs-chip-data-filter-selector-dropdown
            dictionary='{"stringBetween": "Between", "stringOutside": "Outside", "stringBefore": "before", "stringHours": "hours"}' tabindex="0">
            </ptcs-chip-data-filter-selector-dropdown>
        `);
        selector.sortFilters = false;
        selector.customBaseTypesMapping = {default: 'string', infotable: 'unsupported', integer: 'number'};
        selector.data = data;
        selector._renderSubcomponents = true; // Trigger generation of *-case subcomponents
        await PTCS.wait();

        mainDropDown = selector.$['main-drop-down'];
        applyButton = selector.$['apply-button'];
        mainDropDown.selectedIndexes = [dateTimeIndex]; // select the datetime type based filter option
        await PTCS.wait(200);
        container = selector.$['container'];
        dateToBeInserted = new Date('2020-03-11');
        withinValue = 2;
        withinUnit = 'h';
    });

    it('is saving handled correctly & query is generated correctly', async function() {
        this.timeout(5000);

        const expectedQueryObj = {
            filters: {
                type:    'And',
                filters: []
            }
        };

        await PTCS.wait();


        const handler = event => {
            const passedData = event.detail.data;
            const dataOfInterest = passedData && passedData.length && passedData[passedData.length - 1];
            const dataEnteredByUser = dataOfInterest.dataEnteredByUser;
            const isError = dataOfInterest && dataOfInterest.isError;
            const valueEnteredByUser = dataEnteredByUser.operation === 'within' ? withinValue : dateToBeInserted;

            let expectedData = null;
            switch (dataEnteredByUser.operation) {
                case 'within':
                    expectedData = dataEnteredByUser.value;
                    break;
                case 'between':
                    expectedData = dataEnteredByUser.from;
                    break;
                default:
                    expectedData = dataEnteredByUser.date;
            }
            expectedQueryObj.filters.filters.push(getDateQueryChip('datetime', dataCategory.name, dataOfInterest.dataEnteredByUser));
            const expectedQuery = JSON.stringify(expectedQueryObj);
            const selectorQuery = JSON.stringify(selector.query);

            expect(selectorQuery).to.be.eql(expectedQuery, 'selectorQuery');
            expect(valueEnteredByUser).to.be.eql(expectedData, 'valueEnteredByUser');
            expect(isError).to.be.eql(false, 'isError should be false');
            selector.removeEventListener('change', handler);
        };

        const datetimeCaseComp = container.querySelector('#datetime-case');

        // check whether query is not yet ready when none data were provided
        expect(datetimeCaseComp.query).to.be.eql(null);

        mainDropDown.selectedIndexes = [dateTimeIndex];
        await PTCS.wait();

        datetimeCaseComp.dataEnteredByUser = {operation: 'before', date: dateToBeInserted};

        await PTCS.wait();

        selector.addEventListener('change', handler);
        selector.$['apply-button'].click();
        mainDropDown.selectedIndexes = [dateTimeIndex];
        await PTCS.wait(200);
        datetimeCaseComp.dataEnteredByUser = {operation: 'beforeEq', date: dateToBeInserted};

        await PTCS.wait();

        selector.addEventListener('change', handler);
        selector.$['apply-button'].click();
        mainDropDown.selectedIndexes = [dateTimeIndex];
        await PTCS.wait(200);
        datetimeCaseComp.dataEnteredByUser = {operation: 'after', date: dateToBeInserted};

        await PTCS.wait();

        selector.addEventListener('change', handler);
        selector.$['apply-button'].click();
        mainDropDown.selectedIndexes = [dateTimeIndex];
        await PTCS.wait(200);
        datetimeCaseComp.dataEnteredByUser = {operation: 'afterEq', date: dateToBeInserted};

        await PTCS.wait();

        selector.addEventListener('change', handler);
        selector.$['apply-button'].click();
        mainDropDown.selectedIndexes = [dateTimeIndex];
        await PTCS.wait(200);
        datetimeCaseComp.dataEnteredByUser = {operation: 'equals', date: dateToBeInserted};

        await PTCS.wait();

        selector.addEventListener('change', handler);
        selector.$['apply-button'].click();
        mainDropDown.selectedIndexes = [dateTimeIndex];
        await PTCS.wait(200);
        datetimeCaseComp.dataEnteredByUser = {operation: 'notEq', date: dateToBeInserted};

        await PTCS.wait();

        selector.addEventListener('change', handler);
        selector.$['apply-button'].click();
        mainDropDown.selectedIndexes = [dateTimeIndex];
        await PTCS.wait(200);
        datetimeCaseComp.dataEnteredByUser = {operation: 'between', from: dateToBeInserted, to: dateToBeInserted};

        await PTCS.wait();

        selector.addEventListener('change', handler);
        selector.$['apply-button'].click();
        mainDropDown.selectedIndexes = [dateTimeIndex];
        await PTCS.wait(200);
        datetimeCaseComp.dataEnteredByUser = {operation: 'within', value: withinValue, units: withinUnit};

        await PTCS.wait();

        selector.addEventListener('change', handler);
        selector.$['apply-button'].click();
        mainDropDown.selectedIndexes = [dateTimeIndex];
    });

    it('is saving handled correctly & query is generated correctly - expanded mode', async function() {
        const expectedQueryObj = {
            filters: {
                type:    'And',
                filters: []
            }
        };

        mainDropDown.selectedIndexes = [dateTimeIndex]; // select the datetime type based filter option
        await PTCS.wait();

        const handler = event => {
            const passedData = event.detail.data;
            const dataOfInterest = passedData && passedData.length && passedData[passedData.length - 1];
            const dataEnteredByUser = dataOfInterest.dataEnteredByUser;
            const isError = dataOfInterest && dataOfInterest.isError;
            const valueEnteredByUser = dataEnteredByUser.operation === 'within' ? withinValue : dateToBeInserted;

            let expectedData = null;
            switch (dataEnteredByUser.operation) {
                case 'within':
                    expectedData = dataEnteredByUser.value;
                    break;
                case 'between':
                    expectedData = dataEnteredByUser.from;
                    break;
                default:
                    expectedData = dataEnteredByUser.date;
            }
            expectedQueryObj.filters.filters.push(getDateQueryChip('datetime', dataCategory.name, dataOfInterest.dataEnteredByUser));
            const expectedQuery = JSON.stringify(expectedQueryObj);
            const selectorQuery = JSON.stringify(selector.query);

            expect(selectorQuery).to.be.eql(expectedQuery);
            expect(valueEnteredByUser).to.be.eql(expectedData);
            expect(isError).to.be.eql(false);
            selector.removeEventListener('change', handler);
        };

        const datetimeCaseComp = container.querySelector('#datetime-case');

        // check whether query is not yet ready when none data were provided
        expect(datetimeCaseComp.query).to.be.eql(null);

        selector.display = 'expanded'; // expanded mode
        await PTCS.wait();

        mainDropDown.selectedIndexes = [dateTimeIndex];
        await PTCS.wait();

        datetimeCaseComp.dataEnteredByUser = {operation: 'before', date: dateToBeInserted};

        await PTCS.wait();
        selector.addEventListener('change', handler);
        selector.$['apply-button'].click();
        mainDropDown.selectedIndexes = [dateTimeIndex];
        await PTCS.wait();
        datetimeCaseComp.dataEnteredByUser = {operation: 'beforeEq', date: dateToBeInserted};

        await PTCS.wait();
        selector.addEventListener('change', handler);
        selector.$['apply-button'].click();
        mainDropDown.selectedIndexes = [dateTimeIndex];
        await PTCS.wait();
        datetimeCaseComp.dataEnteredByUser = {operation: 'after', date: dateToBeInserted};

        await PTCS.wait();
        selector.addEventListener('change', handler);
        selector.$['apply-button'].click();
        mainDropDown.selectedIndexes = [dateTimeIndex];
        await PTCS.wait();
        datetimeCaseComp.dataEnteredByUser = {operation: 'afterEq', date: dateToBeInserted};

        await PTCS.wait();
        selector.addEventListener('change', handler);
        selector.$['apply-button'].click();
        mainDropDown.selectedIndexes = [dateTimeIndex];
        await PTCS.wait();
        datetimeCaseComp.dataEnteredByUser = {operation: 'equals', date: dateToBeInserted};

        await PTCS.wait();
        selector.addEventListener('change', handler);
        selector.$['apply-button'].click();
        mainDropDown.selectedIndexes = [dateTimeIndex];
        await PTCS.wait();
        datetimeCaseComp.dataEnteredByUser = {operation: 'notEq', date: dateToBeInserted};

        await PTCS.wait();
        selector.addEventListener('change', handler);
        selector.$['apply-button'].click();
        mainDropDown.selectedIndexes = [dateTimeIndex];
        await PTCS.wait(200);
        datetimeCaseComp.dataEnteredByUser = {operation: 'between', from: dateToBeInserted, to: dateToBeInserted};

        await PTCS.wait();

        // In expanded mode we use separate datetime pickers (one each for each end of the range) for 'between' operation
        const dateContainer = datetimeCaseComp.shadowRoot.getElementById('date-container');
        const toDateContainer = datetimeCaseComp.shadowRoot.getElementById('between-container');
        expect(dateContainer.hasAttribute('data-enabled')).to.be.eql(true, 'From datepicker container is visible');
        expect(toDateContainer.hasAttribute('data-enabled')).to.be.eql(true, 'To datepicker container is visible');

        selector.addEventListener('change', handler);
        selector.$['apply-button'].click();
        mainDropDown.selectedIndexes = [dateTimeIndex];
        await PTCS.wait();
        datetimeCaseComp.dataEnteredByUser = {operation: 'within', value: withinValue, units: withinUnit};

        await PTCS.wait();
        selector.addEventListener('change', handler);
        selector.$['apply-button'].click();
        mainDropDown.selectedIndexes = [dateTimeIndex];
        await PTCS.wait();
    });


    it('is field name set correctly', async function() {
        await PTCS.wait();
        mainDropDown.selectedIndexes = [dateTimeIndex]; // select the datetime type based filter option
        await PTCS.wait();

        const datetimeCaseComp = container.querySelector('#datetime-case');

        expect(datetimeCaseComp.queryFieldName()).to.be.eql(dataCategory.name);
    });

    it('does Apply button behave correctly 4', async() => {
        await PTCS.wait();

        expect(applyButton.hasAttribute('disabled')).to.be.eql(true);

        const datetimeCaseComp = container.querySelector('#datetime-case');
        operationsDropDown = datetimeCaseComp.$['drop-down'];
        operationsDropDown.selectedValue = 'equals';
        datePicker = datetimeCaseComp.$['date-picker'];
        datePicker.dateTime = dateToBeInserted;

        await PTCS.wait();

        expect(applyButton.hasAttribute('disabled')).to.be.eql(false);
        datePicker.dateTime = null;

        await PTCS.wait();

        expect(applyButton.hasAttribute('disabled')).to.be.eql(true);
    });

    it('test bad datetime conditions', async function() {
        const datetimeCaseComp = container.querySelector('#datetime-case');

        datetimeCaseComp.dataEnteredByUser = null;

        await PTCS.wait();

        expect(applyButton.hasAttribute('disabled')).to.be.eql(true);
        datetimeCaseComp.dataEnteredByUser = {operation: ''};

        await PTCS.wait();

        expect(applyButton.hasAttribute('disabled')).to.be.eql(true);
        datetimeCaseComp.dataEnteredByUser = {operation: 'between'};

        await PTCS.wait();

        expect(applyButton.hasAttribute('disabled')).to.be.eql(true);
        datetimeCaseComp.dataEnteredByUser = {operation: 'within'};

        await PTCS.wait();

        expect(applyButton.hasAttribute('disabled')).to.be.eql(true);
        datetimeCaseComp.dataEnteredByUser = {operation: 'equals'};

        await PTCS.wait();

        expect(applyButton.hasAttribute('disabled')).to.be.eql(true);
    });
});

describe('<ptcs-chip-data-filter-selector-dropdown>-nothingSelectedYet', () => {
    let noSelectionCaseTextField, selector, mainDropDown, applyButton;

    beforeEach(async() => {
        selector = await fixture(`
            <ptcs-chip-data-filter-selector-dropdown dictionary='{"stringBetween": "Between", "stringOutside": "Outside"}' tabindex="0">
            </ptcs-chip-data-filter-selector-dropdown>
        `);

        selector._renderSubcomponents = true; // Trigger generation of *-case subcomponents
        await selector.updateComplete;

        selector.sortFilters = false;
        mainDropDown = selector.$['main-drop-down'];
        noSelectionCaseTextField = selector.$['no-selection-case-text-field'];
        applyButton = selector.$['apply-button'];
    });

    it('are drop-down, text field, apply disabled when there is no input data', async() => {
        await PTCS.wait();
        expect(mainDropDown.hasAttribute('disabled')).to.be.eql(true, 'main-drop-down');
        expect(noSelectionCaseTextField.hasAttribute('disabled')).to.be.eql(true, 'no-selection-case-text-field is disabled');
        expect(applyButton.hasAttribute('disabled')).to.be.eql(true, 'Apply button is disabled');
    });

    it('are text field & apply disabled once input data has been provided', async() => {
        selector.customBaseTypesMapping = {default: 'string', infotable: 'unsupported', integer: 'number'};
        selector.data = data;

        await PTCS.wait();

        expect(mainDropDown.hasAttribute('disabled')).to.be.eql(false);
        expect(noSelectionCaseTextField.hasAttribute('disabled')).to.be.eql(true);
        expect(applyButton.hasAttribute('disabled')).to.be.eql(true);
    });

    it('is main drop-down populated correctly', done => {
        const mainDropDownRefItems = Object
            .values(data.dataShape.fieldDefinitions)
            .map(extendedOption => new Object({label: extendedOption.name, value: extendedOption.name})).filter(e => {
                return e.value !== 'unsupportedBasedCategory'; // unsupportedBasedCategory will be excluded
            });

        expect(selector.data).to.be.null; // no data yet
        // unhappy path -> setting the wrong data
        selector.data = {
            incorrectField: 'incorrectValue'
        };
        expect(selector.data).to.be.null;

        // happy path
        selector.customBaseTypesMapping = {default: 'string', infotable: 'unsupported', integer: 'number'};
        selector.data = data;
        expect(mainDropDown.items).to.be.eql(mainDropDownRefItems);
        const supportedAndNotSupportedDataType = {
            dataShape: {
                fieldDefinitions: {
                    correctFilterOption: {
                        name:     'textBasedCategory',
                        baseType: 'STRING'
                    },
                    incorrectFilterOption: {
                        name:     'some unsupported type',
                        baseType: 'INFOTABLE'
                    }
                }
            }
        };

        // check case when no customBaseTypesMapping is set
        selector.customBaseTypesMapping = null;
        selector.data = supportedAndNotSupportedDataType;
        expect(mainDropDown.items).to.be.eql([{label: supportedAndNotSupportedDataType.dataShape.fieldDefinitions.correctFilterOption.name,
            value: supportedAndNotSupportedDataType.dataShape.fieldDefinitions.correctFilterOption.name}]);
        done();
    });

    it('is text field updated & apply button enabled after entering data', async() => {
        selector.customBaseTypesMapping = {default: 'string', infotable: 'unsupported', integer: 'number'};
        selector.data = data;
        await PTCS.wait();
        mainDropDown.selectedIndexes = [findIndexForGivenType('string')]; // select the text type based filer option

        // Wait a bit longer here---the callback when the selected indexes has been updated is defined as async, so otherwise
        // it will not perform its last part (setting the disabled state of the 'apply-button' to true) until after the data
        // has been entered, hence failing the 'expect' statement below...
        await PTCS.wait(300);

        expect(selector.shadowRoot.getElementById('apply-button').hasAttribute('disabled')).to.be.eql(true, 'Apply Button should be disabled');

        const stringCaseComp = selector.shadowRoot.getElementById('container').querySelector('#string-case');
        expect(stringCaseComp).to.not.be.eql(null, 'stringCaseComp resolved');

        stringCaseComp.dataEnteredByUser = {operation: 'exact', value: 'exact match'};

        await PTCS.wait(100);

        expect(stringCaseComp.shadowRoot.getElementById('drop-down').selectedValue).to.be.eql('exact');
        expect(stringCaseComp.shadowRoot.getElementById('text-field').shadowRoot.getElementById('input').value).to.be.eql('exact match');

        await PTCS.wait();

        expect(stringCaseComp.isFilled).to.be.eql(true, 'stringCaseComp is filled');

        await selector.updateComplete;

        expect(selector.shadowRoot.getElementById('apply-button').disabled).to.be.eql(false, 'Apply Button should be enabled now');
        selector.shadowRoot.getElementById('apply-button').click();

        await PTCS.wait();

        expect(selector.$['apply-button'].hasAttribute('disabled')).to.be.eql(true, 'Apply button disabled after filter was accepted and processed');
        expect(noSelectionCaseTextField.hasAttribute('disabled')).to.be.eql(true);
    });

    it('localization is working', done => {
        selector.customBaseTypesMapping = {default: 'string', infotable: 'unsupported', integer: 'number'};
        selector.data = dataWithLocalization;
        const idx = findIndexForGivenType('integer', dataWithLocalization);
        const category = findDataCategoryForGivenType('integer', dataWithLocalization);
        expect(mainDropDown.items[idx].label).to.be.eql(category.Title);
        done();
    });

    it('sorting is working', done => {
        selector.customBaseTypesMapping = {default: 'string', infotable: 'unsupported', integer: 'number', yyy: 'xxx'};
        selector.columnFormat = columnFormat;
        selector.sortFilters = true;
        selector.data = dataWithLocalization;

        const origcategoriesCount = Object.keys(dataWithLocalization.dataShape.fieldDefinitions).length;
        expect(mainDropDown.items.length).to.be.eql(origcategoriesCount - 3); // bad type,__showThisField=false, yyy->xxx
        done();
    });

    it('custom column format is excluded since it doesn\'t match dataShape', done => {
        selector.customBaseTypesMapping = {default: 'string', infotable: 'unsupported', integer: 'number', yyy: 'xxx'};
        selector.columnFormat = JSON.stringify({
            TextBasedCategory: {
                name:     'zzzTextBasedCategory',
                baseType: 'STRING'
            }
        });

        selector.data = dataWithLocalization;

        const origcategoriesCount = Object.keys(dataWithLocalization.dataShape.fieldDefinitions).length;
        expect(mainDropDown.items.length).to.be.eql(origcategoriesCount - 7); // bad type, yyy->xxx
        done();
    });

    it('dataTableEditor mashup should load filter fields with valid baseType columns', async() => {
        selector.data = metaData;
        await PTCS.wait();

        const origcategoriesCount = Object.keys(dataWithLocalization.dataShape.fieldDefinitions).length;
        expect(mainDropDown.items.length).to.be.eql(origcategoriesCount - 2); // bad type
    });

    it('loading different dataShapes', done => {
        const dataTwoFields = {
            dataShape: {
                fieldDefinitions: {
                    TextBasedCategory: {
                        name:     'TextBasedCategory',
                        baseType: 'STRING'
                    },
                    TextBasedCategory2: {
                        name:     'TextBasedCategory2',
                        baseType: 'STRING'
                    }
                }
            }
        };
        const dataTwoFields2 = {
            dataShape: {
                fieldDefinitions: {
                    TextBasedCategory: {
                        name:     'TextBasedCategory',
                        baseType: 'STRING'
                    },
                    zzzTextBasedCategory2: {
                        name:     'zzzTextBasedCategory2',
                        baseType: 'STRING'
                    }
                }
            }
        };
        const dataOneField = {
            dataShape: {
                fieldDefinitions: {
                    TextBasedCategory: {
                        name:     'TextBasedCategory',
                        baseType: 'STRING'
                    }
                }
            }
        };
        const dataOneFieldDiffBaseType = {
            dataShape: {
                fieldDefinitions: {
                    TextBasedCategory: {
                        name:     'TextBasedCategory',
                        baseType: 'NUMBER'
                    }
                }
            }
        };
        selector.customBaseTypesMapping = {default: 'string', infotable: 'unsupported', integer: 'number', yyy: 'xxx'};

        selector.data = dataTwoFields;
        expect(mainDropDown.items.length).to.be.eql(Object.keys(dataTwoFields.dataShape.fieldDefinitions).length);

        selector.data = dataTwoFields2;
        expect(mainDropDown.items.length).to.be.eql(Object.keys(dataTwoFields.dataShape.fieldDefinitions).length);

        selector.data = dataOneField;
        expect(mainDropDown.items.length).to.be.eql(Object.keys(dataOneField.dataShape.fieldDefinitions).length);

        selector.data = dataOneFieldDiffBaseType;
        expect(mainDropDown.items.length).to.be.eql(Object.keys(dataOneFieldDiffBaseType.dataShape.fieldDefinitions).length);

        // load 2nd time the same datashape
        selector.data = dataOneFieldDiffBaseType;
        expect(mainDropDown.items.length).to.be.eql(Object.keys(dataOneFieldDiffBaseType.dataShape.fieldDefinitions).length);

        done();
    });

    describe('<ptcs-chip-data-filter-selector-dropdown>-<location-case>', () => {
        const locationIndex = findIndexForGivenType('location');
        let locationCaseComp, container;
        let proximityValueTextField, latitudeValueTextField, longitudeValueTextField;

        beforeEach(async() => {
            selector = await fixture(`
                <ptcs-chip-data-filter-selector-dropdown dictionary='{"stringWithin": "Within", "stringNotWithin": "Not Within",
                "stringMiles": "miles", "stringKilometers": "kilometers", "stringNauticalMiles": "nautical miles"}' tabindex="0">
                </ptcs-chip-data-filter-selector-dropdown>`
            );
            selector.sortFilters = false;
            selector.customBaseTypesMapping = {default: 'string', infotable: 'unsupported', integer: 'number'};
            selector.data = data;
            selector._renderSubcomponents = true; // Trigger generation of *-case subcomponents

            mainDropDown = selector.$['main-drop-down'];
            mainDropDown.selectedIndexes = [locationIndex]; // select the location based filter option
            applyButton = selector.$['apply-button'];
            container = selector.shadowRoot.getElementById('container');
        });

        it('is saving handled properly & query generated correctly', async function() {

            let expectedLocationCaseData, expectedQuery;
            const queryObj = {
                filters: {
                    type:    'And',
                    filters: []
                }
            };

            const handler = event => {
                const passedData = event.detail.data;
                const dataOfInterest = passedData && passedData.length && passedData[passedData.length - 1];
                const dataEnteredByUser = dataOfInterest && dataOfInterest.dataEnteredByUser;
                const isError = dataOfInterest && dataOfInterest.isError;
                const selectorQuery = JSON.stringify(selector.query);

                expect(dataEnteredByUser).to.be.eql(expectedLocationCaseData);
                expect(selectorQuery).to.be.eql(expectedQuery);
                expect(isError).to.be.eql(false);
                selector.removeEventListener('change', handler);
            };

            await PTCS.wait();

            const dataCategory = findDataCategoryForGivenType('location');

            locationCaseComp = container.querySelector('#location-case');
            proximityValueTextField = locationCaseComp.shadowRoot.getElementById('proximity-value');
            latitudeValueTextField = locationCaseComp.shadowRoot.getElementById('latitude-text-field');
            longitudeValueTextField = locationCaseComp.shadowRoot.getElementById('longitude-text-field');

            expect(proximityValueTextField.text).to.be.eql('');
            expect(latitudeValueTextField.text).to.be.eql('');
            expect(longitudeValueTextField.text).to.be.eql('');

            const firstValToBeInserted = '10';
            const secondValToBeInserted = '11';
            const thirdValToBeInserted = '22';

            let proximityTypeItemSelection = 'NEAR';
            let proximityUnitItemSelection = 'M';

            expectedLocationCaseData = {
                type:      proximityTypeItemSelection,
                value:     firstValToBeInserted,
                units:     proximityUnitItemSelection,
                latitude:  secondValToBeInserted,
                longitude: thirdValToBeInserted
            };

            container = selector.shadowRoot.getElementById('container');
            locationCaseComp = container.querySelector('#location-case');
            proximityValueTextField = locationCaseComp.shadowRoot.getElementById('proximity-value');
            latitudeValueTextField = locationCaseComp.shadowRoot.getElementById('latitude-text-field');
            longitudeValueTextField = locationCaseComp.shadowRoot.getElementById('longitude-text-field');

            mainDropDown.selectedIndexes = [locationIndex];
            await PTCS.wait(200);

            locationCaseComp.dataEnteredByUser = expectedLocationCaseData;
            await PTCS.wait();

            queryObj.filters.filters.push({
                fieldName: dataCategory.name,
                type:      'NEAR',
                location:  {latitude: '11', longitude: '22', elevation: 0, units: 'WGS84'},
                distance:  '10',
                units:     'M'
            });
            expectedQuery = JSON.stringify(queryObj);

            const enterPressEvent = new KeyboardEvent('keyup', {key: 'Enter'});
            selector.addEventListener('change', handler);
            latitudeValueTextField = locationCaseComp.shadowRoot.getElementById('latitude-text-field');
            latitudeValueTextField.dispatchEvent(enterPressEvent);
            await PTCS.wait();

            mainDropDown.selectedIndexes = [locationIndex];

            await PTCS.wait(200);

            expect(proximityValueTextField.text).to.be.eql('');
            expect(latitudeValueTextField.text).to.be.eql('');
            expect(longitudeValueTextField.text).to.be.eql('');

            proximityTypeItemSelection = 'NOTNEAR';
            proximityUnitItemSelection = 'K';
            expectedLocationCaseData = {
                type:      proximityTypeItemSelection,
                value:     firstValToBeInserted,
                units:     proximityUnitItemSelection,
                latitude:  secondValToBeInserted,
                longitude: thirdValToBeInserted
            };

            mainDropDown.selectedIndexes = [locationIndex];
            await PTCS.wait(200);

            locationCaseComp.dataEnteredByUser = expectedLocationCaseData;
            await PTCS.wait();

            queryObj.filters.filters.push({
                fieldName: dataCategory.name,
                type:      'NOTNEAR',
                location:  {latitude: '11', longitude: '22', elevation: 0, units: 'WGS84'},
                distance:  '10',
                units:     'K'
            });
            expectedQuery = JSON.stringify(queryObj);

            selector.addEventListener('change', handler);
            latitudeValueTextField.dispatchEvent(enterPressEvent);
            mainDropDown.selectedIndexes = [locationIndex];

            await PTCS.wait(200);

            expect(proximityValueTextField.text).to.be.eql('');
            expect(latitudeValueTextField.text).to.be.eql('');
            expect(longitudeValueTextField.text).to.be.eql('');

            proximityTypeItemSelection = 'NOTNEAR';
            proximityUnitItemSelection = 'N';
            expectedLocationCaseData = {
                type:      proximityTypeItemSelection,
                value:     firstValToBeInserted,
                units:     proximityUnitItemSelection,
                latitude:  secondValToBeInserted,
                longitude: thirdValToBeInserted
            };

            mainDropDown.selectedIndexes = [locationIndex];
            await PTCS.wait(200);

            locationCaseComp.dataEnteredByUser = expectedLocationCaseData;
            await PTCS.wait();

            queryObj.filters.filters.push({
                fieldName: dataCategory.name,
                type:      'NOTNEAR',
                location:  {latitude: '11', longitude: '22', elevation: 0, units: 'WGS84'},
                distance:  '10',
                units:     'N'
            });
            expectedQuery = JSON.stringify(queryObj);

            selector.addEventListener('change', handler);
            latitudeValueTextField.dispatchEvent(enterPressEvent);
            mainDropDown.selectedIndexes = [locationIndex];
        });
    });
});
